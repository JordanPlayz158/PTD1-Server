package xyz.jordanplayz158.ptd.server.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.orm.Setting
import xyz.jordanplayz158.ptd.server.common.orm.Settings
import xyz.jordanplayz158.ptd.server.dataSource
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.min
import kotlin.system.exitProcess

var isMigrating = false
var migrationError: String? = null
var totalRows: Int = 0
var currentRow: Int = 0
var cancelingMigration = false

val FirstRunDatabaseMigrationPlugin = createApplicationPlugin(name = "FirstRunDatabaseMigration") {
    onCall { call ->
        if(call.request.uri.startsWith("/assets"))
            return@onCall

        if(call.request.httpMethod === HttpMethod.Get) {
            call.respond(ThymeleafContent("databaseMigration/index", mapOf()/*, csrfMapOf(call.sessions)*/))
            return@onCall
        }

        if(call.request.httpMethod === HttpMethod.Post) {
            val body = call.receiveParameters()

            if(body["progress"] !== null) {
                if(migrationError !== null) {
                    call.respondText(migrationError!!)
                    return@onCall
                }

                call.respondText(currentRow.toString())
                return@onCall
            }

            if(body["isMigrating"] === null) {
                if(isMigrating) {
                    call.respond(ThymeleafContent("databaseMigration/index", mapOf("reasons" to listOf("Migration canceled."))))
                    call.application.log.info("Migration canceling.")
                    cancelingMigration = true
                    return@onCall
                }

                call.application.log.info("Not migrating. Updating key 'DB_MIGRATION_ASKED' to 'TRUE' and exiting. After restart the server should work.")
                transaction {
                    Setting.new {
                        key = "DB_MIGRATION_ASKED"
                        value = "TRUE"
                    }
                }
                doShutdown(call, 0)
                return@onCall
            }

            if(isMigrating) {
                call.respond(ThymeleafContent("databaseMigration/index", mapOf("reasons" to listOf("Migration in progress!"))))
                return@onCall
            }

            call.application.log.info("Migrating...")
            val host = body["host"]
            val port = body["port"]
            val name = body["name"]
            val username = body["username"]
            val password = body["password"]

            val reasons = ArrayList<String>()

            if(!nullOrBlank(reasons, "host", host) && isInetAddress(host) != null)
                reasons.add("'host' is not a valid IPv4 or IPv6 address!")

            if(!nullOrBlank(reasons, "port", port)) {
                try {
                    if(port?.toInt()!! <= 0)
                        reasons.add("'port' is not a valid port, ports cannot be negative or zero!")
                } catch (e: NumberFormatException) {
                    reasons.add("'port' is not a valid integer!")
                }
            }

            nullOrBlank(reasons, "name", name)
            nullOrBlank(reasons, "username", username)
            nullOrBlank(reasons, "password", password)


            if(reasons.size > 0) {
                call.respond(ThymeleafContent("databaseMigration/index", mapOf("reasons" to reasons)))
                return@onCall
            }

            cancelingMigration = false
            isMigrating = true

            var oldConn: Connection? = null
            try {
                oldConn = DriverManager.getConnection("jdbc:mysql://$host:$port/$name", username, password)

                val tables =
                    mutableMapOf("achievements" to 0, "pokemon" to 0, "saves" to 0, "save_items" to 0, "users" to 0)
                for ((table, _) in tables) {
                    val totalRowsStmt = oldConn.createStatement()

                    val totalRowsRs = totalRowsStmt.executeQuery("SELECT COUNT(*) FROM $table")
                    totalRowsRs.next()
                    val numberOfRows = totalRowsRs.getInt(1)

                    totalRows += numberOfRows
                    tables[table] = numberOfRows

                    totalRowsRs.close()
                    totalRowsStmt.close()
                }

                call.respond(ThymeleafContent("databaseMigration/index", mapOf("totalRows" to totalRows)))


                Thread {
                    var newConn: Connection? = null
                    var userId = 0L
                    var achievementId = 0L
                    var saveId = 0L
                    var saveItemsOffset = 0
                    var pokemonId = 0L
                    try {
                        newConn = dataSource.connection
                        newConn.autoCommit = false

                        val oldUsersStmt = oldConn.prepareStatement("SELECT * FROM users WHERE id >= ? ORDER BY id LIMIT 1000")
                        val newUsersStmt = newConn.prepareStatement("INSERT INTO users(id, email, role_id, password, dex," +
                                " shiny_dex, shadow_dex, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                        for (i in 0..tables["users"]!! step 1000) {
                            if(cancelingMigration) {
                                throw Exception("Migration canceled")
                            }

                            oldUsersStmt.setLong(1, userId + 1)
                            val oldUsersRS = oldUsersStmt.executeQuery()

                            while(oldUsersRS.next()) {
                                val id = oldUsersRS.getLong("id")
                                userId = id
                                newUsersStmt.setLong(1, id)
                                newUsersStmt.setString(2, oldUsersRS.getString("email"))

                                val roleId = oldUsersRS.getLong("role_id")
                                newUsersStmt.setLong(3, if(roleId != 0L) roleId else 1)
                                newUsersStmt.setString(4, oldUsersRS.getString("password"))

                                val dex = oldUsersRS.getString("dex")
                                newUsersStmt.setString(5, if(dex !== null) dex else "")

                                val shinyDex = oldUsersRS.getString("shinyDex")
                                newUsersStmt.setString(6, if(shinyDex !== null) shinyDex else "")

                                val shadowDex = oldUsersRS.getString("shadowDex")
                                newUsersStmt.setString(7, if(shadowDex !== null) shadowDex else "")
                                newUsersStmt.setTimestamp(8, oldUsersRS.getTimestamp("created_at"))
                                newUsersStmt.setTimestamp(9, oldUsersRS.getTimestamp("updated_at"))
                                currentRow += newUsersStmt.executeUpdate()
                            }
                            oldUsersRS.close()
                        }
                        newUsersStmt.close()
                        oldUsersStmt.close()

                        val oldAchievementStmt = oldConn.prepareStatement("SELECT * FROM achievements WHERE id >= ? ORDER BY id LIMIT 1000")
                        val newAchievementStmt = newConn.prepareStatement("INSERT INTO achievements(id, user_id, shiny_hunter_rattata," +
                                " shiny_hunter_pidgey, shiny_hunter_geodude, shiny_hunter_zubat, star_wars, no_advantage, win_without_wind," +
                                " needs_more_candy, the_hard_way, pewter_challenge, cerulean_challenge, vermillion_challenge," +
                                " celadon_challenge, saffron_city_challenge, fuchsia_gym_challenge, cinnabar_gym_challenge," +
                                " viridian_city_challenge, created_at, updated_at)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                        for (i in 0..tables["achievements"]!! step 1000) {
                            if(cancelingMigration) {
                                throw Exception("Migration canceled")
                            }

                            oldAchievementStmt.setLong(1, achievementId + 1)
                            val oldAchievementRS = oldAchievementStmt.executeQuery()

                            while(oldAchievementRS.next()) {
                                val id = oldAchievementRS.getLong("id")
                                achievementId = id
                                newAchievementStmt.setLong(1, id)
                                newAchievementStmt.setLong(2, oldAchievementRS.getLong("user_id"))

                                val shinyHunter = oldAchievementRS.getString("one").trim().chunked(1).toMutableList()
                                while(shinyHunter.size < 4) {
                                    shinyHunter.add("0")
                                }
                                newAchievementStmt.setInt(3, shinyHunter[0].toInt())
                                newAchievementStmt.setInt(4, shinyHunter[1].toInt())
                                newAchievementStmt.setInt(5, shinyHunter[2].toInt())
                                newAchievementStmt.setInt(6, shinyHunter[3].toInt())

                                newAchievementStmt.setInt(7, oldAchievementRS.getInt("two"))
                                newAchievementStmt.setInt(8, oldAchievementRS.getInt("three"))
                                newAchievementStmt.setInt(9, oldAchievementRS.getInt("four"))
                                newAchievementStmt.setInt(10, oldAchievementRS.getInt("five"))
                                newAchievementStmt.setInt(11, oldAchievementRS.getInt("six"))
                                newAchievementStmt.setInt(12, oldAchievementRS.getInt("seven"))
                                newAchievementStmt.setInt(13, oldAchievementRS.getInt("eight"))
                                newAchievementStmt.setInt(14, oldAchievementRS.getInt("nine"))
                                newAchievementStmt.setInt(15, oldAchievementRS.getInt("ten"))
                                newAchievementStmt.setInt(16, oldAchievementRS.getInt("eleven"))
                                newAchievementStmt.setInt(17, oldAchievementRS.getInt("twelve"))
                                newAchievementStmt.setInt(18, oldAchievementRS.getInt("thirteen"))
                                newAchievementStmt.setInt(19, oldAchievementRS.getInt("fourteen"))
                                newAchievementStmt.setTimestamp(20, oldAchievementRS.getTimestamp("created_at"))
                                newAchievementStmt.setTimestamp(21, oldAchievementRS.getTimestamp("updated_at"))
                                currentRow += newAchievementStmt.executeUpdate()
                            }
                            oldAchievementRS.close()
                        }
                        newAchievementStmt.close()
                        oldAchievementStmt.close()

                        val savesPopulatedPerUser = HashMap<Long, ArrayList<Int>>()
                        val oldSavesStmt = oldConn.prepareStatement("SELECT * FROM saves WHERE id >= ? ORDER BY id LIMIT 1000")
                        val newSavesStmt = newConn.prepareStatement("INSERT INTO saves(id, user_id, number, levels_completed," +
                                " levels_started, nickname, badges, avatar, has_flash, challenge, money, npc_trade, version, created_at, updated_at)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                        for (i in 0..tables["saves"]!! step 1000) {
                            if(cancelingMigration) {
                                throw Exception("Migration canceled.")
                            }

                            oldSavesStmt.setLong(1, saveId + 1)
                            val oldSavesRS = oldSavesStmt.executeQuery()

                            while(oldSavesRS.next()) {
                                val id = oldSavesRS.getLong("id")
                                saveId = id
                                newSavesStmt.setLong(1, id)

                                val oldUserId = oldSavesRS.getLong("user_id")
                                newSavesStmt.setLong(2, oldUserId)

                                val number = oldSavesRS.getInt("num")
                                newSavesStmt.setInt(3, number)

                                savesPopulatedPerUser.putIfAbsent(oldUserId, ArrayList())
                                savesPopulatedPerUser[oldUserId]?.add(number)

                                newSavesStmt.setInt(4, oldSavesRS.getInt("advanced"))
                                newSavesStmt.setInt(5, oldSavesRS.getInt("advanced_a"))

                                val nickname = oldSavesRS.getString("nickname")
                                newSavesStmt.setString(6, if(nickname !== null && nickname.length <= 8) nickname else "Satoshi")
                                newSavesStmt.setInt(7, oldSavesRS.getInt("badges"))

                                val avatar = oldSavesRS.getString("avatar")
                                newSavesStmt.setString(8, if(avatar !== null) avatar else "none")
                                newSavesStmt.setInt(9, oldSavesRS.getInt("classic"))
                                newSavesStmt.setInt(10, oldSavesRS.getInt("challenge"))
                                newSavesStmt.setLong(11, oldSavesRS.getLong("money"))
                                newSavesStmt.setInt(12, oldSavesRS.getInt("npcTrade"))
                                newSavesStmt.setInt(13, oldSavesRS.getInt("version"))
                                newSavesStmt.setTimestamp(14, oldSavesRS.getTimestamp("created_at"))
                                newSavesStmt.setTimestamp(15, oldSavesRS.getTimestamp("updated_at"))
                                currentRow += newSavesStmt.executeUpdate()
                            }
                            oldSavesRS.close()
                        }

                        savesPopulatedPerUser.filter { save -> save.value.size < 3 }.forEach { save ->
                            val numbers = arrayListOf(0, 1, 2)

                            save.value.forEach{usedNumber -> numbers.remove(usedNumber)}

                            numbers.forEach {number ->
                                newSavesStmt.setLong(1, saveId++)
                                newSavesStmt.setLong(2, save.key)
                                newSavesStmt.setInt(3, number)
                                newSavesStmt.setInt(4, 0)
                                newSavesStmt.setInt(5, 0)

                                newSavesStmt.setString(6, "Satoshi")
                                newSavesStmt.setInt(7, 0)

                                newSavesStmt.setString(8, "none")
                                newSavesStmt.setInt(9, 0)
                                newSavesStmt.setInt(10, 0)
                                newSavesStmt.setLong(11, 50)
                                newSavesStmt.setInt(12, 0)
                                newSavesStmt.setInt(13, 1)
                                newSavesStmt.setTimestamp(14, Timestamp(Instant.now().epochSecond))
                                newSavesStmt.setTimestamp(15, null)
                            }
                        }

                        newSavesStmt.close()
                        oldSavesStmt.close()

                        val oldSaveItemsStmt = oldConn.prepareStatement("SELECT save_id, item, COUNT(item) FROM save_items" +
                                " GROUP BY save_id, item ORDER BY save_id LIMIT 1000 OFFSET ?")
                        val newSaveItemsStmt = newConn.prepareStatement("INSERT INTO save_items(save_id, item, count) VALUES (?, ?, ?)")
                        for (i in 0..tables["save_items"]!! step 1000) {
                            if(cancelingMigration) {
                                throw Exception("Migration canceled")
                            }

                            saveItemsOffset = i
                            oldSaveItemsStmt.setInt(1, i)
                            val oldSaveItemsRS = oldSaveItemsStmt.executeQuery()

                            while(oldSaveItemsRS.next()) {
                                newSaveItemsStmt.setLong(1, oldSaveItemsRS.getLong("save_id"))
                                newSaveItemsStmt.setInt(2, oldSaveItemsRS.getInt("item"))
                                newSaveItemsStmt.setInt(3, min(255, oldSaveItemsRS.getInt("COUNT(item)")))
                                currentRow += newSaveItemsStmt.executeUpdate()
                            }
                            oldSaveItemsRS.close()
                        }
                        newSaveItemsStmt.close()
                        oldSaveItemsStmt.close()

                        val oldPokemonStmt = oldConn.prepareStatement("SELECT * FROM pokemon WHERE id >= ? ORDER BY id LIMIT 1000")
                        val newPokemonStmt = newConn.prepareStatement("INSERT INTO pokemon(id, save_id, swf_id, number, nickname," +
                                " experience, level, move_1, move_2, move_3, move_4, move_selected, ability, target_type, tag, position," +
                                " rarity, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                        for (i in 0..tables["pokemon"]!! step 1000) {
                            if(cancelingMigration) {
                                throw Exception("Migration canceled")
                            }

                            oldPokemonStmt.setLong(1, pokemonId + 1)
                            val oldPokemonRS = oldPokemonStmt.executeQuery()

                            while(oldPokemonRS.next()) {
                                val id = oldPokemonRS.getLong("id")
                                pokemonId = id
                                newPokemonStmt.setLong(1, id)
                                newPokemonStmt.setLong(2, oldPokemonRS.getLong("save_id"))
                                newPokemonStmt.setInt(3, oldPokemonRS.getInt("pId"))

                                newPokemonStmt.setInt(4, min(65535, oldPokemonRS.getInt("pNum")))

                                val nickname = oldPokemonRS.getString("nickname")
                                newPokemonStmt.setString(5, nickname.substring(0, min(30, nickname.length)))
                                newPokemonStmt.setInt(6, oldPokemonRS.getInt("exp"))
                                newPokemonStmt.setInt(7, oldPokemonRS.getInt("lvl"))
                                newPokemonStmt.setInt(8, oldPokemonRS.getInt("m1"))
                                newPokemonStmt.setInt(9, oldPokemonRS.getInt("m2"))
                                newPokemonStmt.setInt(10, oldPokemonRS.getInt("m3"))
                                newPokemonStmt.setInt(11, oldPokemonRS.getInt("m4"))
                                newPokemonStmt.setInt(12, oldPokemonRS.getInt("mSel"))
                                newPokemonStmt.setInt(13, oldPokemonRS.getInt("ability"))
                                newPokemonStmt.setInt(14, oldPokemonRS.getInt("targetType"))

                                val tag = oldPokemonRS.getString("tag")
                                newPokemonStmt.setString(15, if(tag !== null) tag else "h")
                                newPokemonStmt.setInt(16, oldPokemonRS.getInt("pos"))
                                newPokemonStmt.setInt(17, oldPokemonRS.getInt("shiny"))
                                newPokemonStmt.setTimestamp(18, oldPokemonRS.getTimestamp("created_at"))
                                newPokemonStmt.setTimestamp(19, oldPokemonRS.getTimestamp("updated_at"))
                                currentRow += newPokemonStmt.executeUpdate()
                            }
                            oldPokemonRS.close()
                        }
                        newPokemonStmt.close()
                        oldPokemonStmt.close()

                        newConn.commit()
                    } catch (e: Exception) {
                        oldConn?.close()
                        newConn?.rollback()
                        newConn?.close()

                        val rowProgress = "Last User ID: $userId, Total Rows in users: ${tables["users"]}\n" +
                                "Last Achievement ID: $achievementId, Total Rows in achievements: ${tables["achievements"]}\n" +
                                "Last Save ID: $saveId, Total Rows in saves: ${tables["saves"]}\n" +
                                "Last Save_items OFFSET: $saveItemsOffset, Total Rows in save_items: ${tables["save_items"]}\n" +
                                "Last Pokemon ID: $pokemonId, Total Rows in pokemon: ${tables["pokemon"]}\n"

                        call.application.log.error(rowProgress)
                        e.printStackTrace()
                        migrationError = rowProgress + stackTraceToString(e)
                        isMigrating = false
                        return@Thread
                    }

                    oldConn?.close()
                    newConn?.close()

                    currentRow = totalRows
                    isMigrating = false
                    call.application.log.info("Migration Successful! Manual Restart Required.")
                    transaction {
                        Setting.find(Settings.key eq "DB_MIGRATION_ASKED").first().value = "TRUE"
                    }
                }.start()
            } catch (e: Exception) {
                oldConn?.close()
                call.respond(ThymeleafContent("databaseMigration/index", mapOf("reasons" to listOf(stackTraceToString(e)))))
                isMigrating = false
                return@onCall
            }
        }
    }
}

fun nullOrBlank(reasons: ArrayList<String>, name: String, value: String?): Boolean {
    if(value.isNullOrBlank()) {
        reasons.add("'$name' is missing or blank!")
        return true
    }

    return false
}

fun stackTraceToString(throwable: Throwable): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString()
    pw.close()
    sw.close()
    return stackTrace
}

fun isInetAddress(address: String?) : InetAddress? {
    if (address.isNullOrBlank()) {
        return null
    }

    return try {
        InetAddress.getByName(address)
    } catch (_: UnknownHostException) {
        null
    }
}


// Grabbed from https://github.com/ktorio/ktor/blob/main/ktor-server/ktor-server-host-common/jvm/src/io/ktor/server/engine/ShutDownUrl.kt
//   to see how proper shutdown was done
/**
 * Shuts down an application using the specified [call].
 */
suspend fun doShutdown(call: ApplicationCall, exitCode: Int) {
    call.application.log.warn("doShutdown was called: server is going down")
    val application = call.application
    val environment = application.environment

    val latch = CompletableDeferred<Nothing>()
    call.application.launch {
        latch.join()

        environment.monitor.raise(ApplicationStopPreparing, environment)
        if (environment is ApplicationEngineEnvironment) {
            environment.stop()
        } else {
            application.dispose()
        }

        exitProcess(exitCode)
    }

    try {
        call.respond(HttpStatusCode.Gone)
    } finally {
        latch.cancel()
    }
}