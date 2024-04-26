package xyz.jordanplayz158.ptd.server.common

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.orm.Setting
import xyz.jordanplayz158.ptd.server.dataSource
import xyz.jordanplayz158.ptd.server.databaseServer
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


object InetAddressNullableSerializer : KSerializer<InetAddress?>,TypeAdapter<InetAddress?>() {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InetAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetAddress?) {
        encoder.encodeString(value?.hostName ?: "")
    }

    override fun deserialize(decoder: Decoder): InetAddress? {
        return isInetAddress(decoder.decodeString())
    }

    override fun write(p0: JsonWriter, p1: InetAddress?) {
        p0.value(p1?.hostName ?: "")
    }

    override fun read(p0: JsonReader): InetAddress? {
        return isInetAddress(p0.nextString())
    }
}

@Serializable
data class DatabaseMigration(
    val isMigrating: Boolean = false,
    @Serializable(with = InetAddressNullableSerializer::class)
    val host: InetAddress?, // NotNull
    val port: Int?, // Min(value = 1)
    val name: String?, // NotBlank
    val username: String?, // NotBlank
    val password: String?, // NotBlank
)

val FirstRunDatabaseMigrationPlugin = createApplicationPlugin(name = "FirstRunDatabaseMigration") {
    val logger = application.log

    fun migrationAsked() {
        transaction {
            Setting.new {
                key = "DB_MIGRATION_ASKED"
                value = "TRUE"
            }
        }
    }

    var migrationError = ""
    var currentRow = 0
    suspend fun progress(call: ApplicationCall) {
        call.respondText(migrationError.ifEmpty { currentRow.toString() })
    }

    var isMigrating = false
    var cancelingMigration: Boolean
    var intercept = true // For first run, this is so you don't need to reload the server after migration
    suspend fun cancel(call: ApplicationCall) {
        if(isMigrating) {
            logger.info("Migration canceling.")
            cancelingMigration = true

            return pageReasons(call, "Migration canceled.")
        }

        logger.info("Not migrating. Updating key 'DB_MIGRATION_ASKED' to 'TRUE'.")

        migrationAsked()

        intercept = false
        return call.respond(ThymeleafContent("redirect", mapOf("redirect" to "/")))
    }

    var totalRows = 0
    onCall { call ->
        val url = call.request.uri

        if(url.startsWith("/assets") || !intercept) return@onCall

        val method = call.request.httpMethod

        if(method === HttpMethod.Get) return@onCall if (url.endsWith("progress")) progress(call) else page(call)

        if(method === HttpMethod.Post) {
            val (dataIsMigrating, inetAddress, port, name, username, password) = call.receive<DatabaseMigration>()

            if (!dataIsMigrating) return@onCall cancel(call)

            if(isMigrating) {
                pageReasons(call, "Migration in progress!")
                return@onCall
            }

            val values = HashMap<String, String>()
            val reasons = ArrayList<String>()

            if(inetAddress === null) reasons.add("'host' is not a valid IPv4 or IPv6 address!") else values["host"] = inetAddress.hostName

            if (port === null) reasons.add("'port' is not a valid integer!") else {
                if(port <= 0) reasons.add("'port' is not a valid port, ports cannot be negative or zero!") else values["port"] = "$port"
            }

            if (name === null) reasons.add("'name' is blank!") else values["name"] = name
            if (username === null) reasons.add("'username' is blank!") else values["username"] = username
            if (password === null) reasons.add("'password' is blank!")

            if(reasons.isNotEmpty()) {
                pageForm(call, values, *reasons.toTypedArray())
                return@onCall
            }

            val host = inetAddress!!.hostName

            logger.info("Migrating...")

            cancelingMigration = false
            isMigrating = true

            var oldConn: Connection? = null
            try {
                oldConn = DriverManager.getConnection("jdbc:mariadb://$host:$port/$name", username, password)

                val tables =
                    mutableMapOf("achievements" to 0, "pokemon" to 0, "saves" to 0, "save_items" to 0, "users" to 0)
                for ((table, _) in tables) {
                    oldConn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                            rs.next()
                            val numberOfRows = rs.getInt(1)

                            totalRows += numberOfRows
                            tables[table] = numberOfRows
                        }
                    }
                }

                pageForm(call, values, "totalRows" to totalRows)

                Thread {
                    var newConn: Connection? = null
                    var oldUserId = 0L
                    var achievementId = 0L
                    var saveId = 0L
                    var saveItemsOffset = 0
                    var pokemonId = 0L
                    try {
                        newConn = dataSource.connection
                        newConn.autoCommit = false

                        val chunking = 1000
                        oldConn.prepareStatement("SELECT * FROM users WHERE id >= ? ORDER BY id LIMIT $chunking")
                            .use { oldStmt ->
                                newConn.prepareStatement("INSERT INTO users(email, role_id, password) VALUES (?, ?, ?)")
                                    .use { newStmt ->
                                        newConn.prepareStatement(
                                            "INSERT INTO ptd1_users(id, user_id, dex," +
                                                    " shiny_dex, shadow_dex, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                        ).use { newPtd1Stmt ->
                                            for (i in 0..tables["users"]!! step chunking) {
                                                if (cancelingMigration) {
                                                    throw Exception("Migration canceled")
                                                }

                                                oldStmt.setLong(1, oldUserId + 1)
                                                oldStmt.executeQuery().use { rs ->
                                                    while (rs.next()) {
                                                        newStmt.setString(1, rs.getString("email"))

                                                        val roleId = when (rs.getLong("role_id")) {
                                                            2L -> 1L
                                                            else -> 100L
                                                        }

                                                        newStmt.setLong(2, roleId)
                                                        newStmt.setString(3, rs.getString("password"))
                                                        newStmt.executeUpdate()

                                                        oldUserId = rs.getLong("id")
                                                        newPtd1Stmt.setLong(1, oldUserId)

                                                        newStmt.generatedKeys.next()

                                                        newPtd1Stmt.setLong(2, newStmt.generatedKeys.getLong(1))
                                                        newPtd1Stmt.setString(3, rs.getString("dex") ?: "")
                                                        newPtd1Stmt.setString(4, rs.getString("shinyDex") ?: "")
                                                        newPtd1Stmt.setString(5, rs.getString("shadowDex") ?: "")
                                                        newPtd1Stmt.setTimestamp(6, rs.getTimestamp("created_at"))
                                                        newPtd1Stmt.setTimestamp(7, rs.getTimestamp("updated_at"))

                                                        currentRow += newPtd1Stmt.executeUpdate()
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }


                        oldConn.prepareStatement("SELECT * FROM achievements WHERE id >= ? ORDER BY id LIMIT $chunking")
                            .use { oldStmt ->
                                newConn.prepareStatement(
                                    "INSERT INTO ptd1_achievements(id, user_id, shiny_hunter_rattata," +
                                            " shiny_hunter_pidgey, shiny_hunter_geodude, shiny_hunter_zubat, star_wars, no_advantage, win_without_wind," +
                                            " needs_more_candy, the_hard_way, pewter_challenge, cerulean_challenge, vermillion_challenge," +
                                            " celadon_challenge, saffron_city_challenge, fuchsia_gym_challenge, cinnabar_gym_challenge," +
                                            " viridian_city_challenge, created_at, updated_at)" +
                                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                ).use { newStmt ->
                                    for (i in 0..tables["achievements"]!! step chunking) {
                                        if (cancelingMigration) {
                                            throw Exception("Migration canceled")
                                        }

                                        oldStmt.setLong(1, achievementId + 1)
                                        oldStmt.executeQuery().use { rs ->
                                            while (rs.next()) {
                                                val id = rs.getLong("id")
                                                achievementId = id
                                                newStmt.setLong(1, id)
                                                newStmt.setLong(2, rs.getLong("user_id"))

                                                val shinyHunter =
                                                    rs.getString("one").trim().chunked(1).toMutableList()
                                                while (shinyHunter.size < 4) {
                                                    shinyHunter.add("0")
                                                }
                                                newStmt.setInt(3, shinyHunter[0].toInt())
                                                newStmt.setInt(4, shinyHunter[1].toInt())
                                                newStmt.setInt(5, shinyHunter[2].toInt())
                                                newStmt.setInt(6, shinyHunter[3].toInt())

                                                newStmt.setInt(7, rs.getInt("two"))
                                                newStmt.setInt(8, rs.getInt("three"))
                                                newStmt.setInt(9, rs.getInt("four"))
                                                newStmt.setInt(10, rs.getInt("five"))
                                                newStmt.setInt(11, rs.getInt("six"))
                                                newStmt.setInt(12, rs.getInt("seven"))
                                                newStmt.setInt(13, rs.getInt("eight"))
                                                newStmt.setInt(14, rs.getInt("nine"))
                                                newStmt.setInt(15, rs.getInt("ten"))
                                                newStmt.setInt(16, rs.getInt("eleven"))
                                                newStmt.setInt(17, rs.getInt("twelve"))
                                                newStmt.setInt(18, rs.getInt("thirteen"))
                                                newStmt.setInt(19, rs.getInt("fourteen"))
                                                newStmt.setTimestamp(
                                                    20,
                                                    rs.getTimestamp("created_at")
                                                        ?: Timestamp(Instant.now().epochSecond)
                                                )
                                                newStmt.setTimestamp(21, rs.getTimestamp("updated_at"))
                                                currentRow += newStmt.executeUpdate()
                                            }
                                        }
                                    }
                                }
                            }


                        val savesPopulatedPerUser = HashMap<Long, ArrayList<Int>>()
                        oldConn.prepareStatement("SELECT * FROM saves WHERE id >= ? ORDER BY id LIMIT $chunking")
                            .use { oldStmt ->
                                newConn.prepareStatement(
                                    "INSERT INTO ptd1_saves(id, user_id, number, levels_completed," +
                                            " levels_started, nickname, badges, avatar, has_flash, challenge, money, npc_trade, version, created_at, updated_at)" +
                                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                ).use { newStmt ->
                                    for (i in 0..tables["saves"]!! step chunking) {
                                        if (cancelingMigration) {
                                            throw Exception("Migration canceled.")
                                        }

                                        oldStmt.setLong(1, saveId + 1)
                                        oldStmt.executeQuery().use { rs ->
                                            while (rs.next()) {
                                                val id = rs.getLong("id")
                                                saveId = id
                                                newStmt.setLong(1, id)

                                                val oldUserId = rs.getLong("user_id")
                                                newStmt.setLong(2, oldUserId)

                                                val number = rs.getInt("num")
                                                newStmt.setInt(3, number)

                                                savesPopulatedPerUser.putIfAbsent(oldUserId, ArrayList())
                                                savesPopulatedPerUser[oldUserId]?.add(number)

                                                newStmt.setInt(4, rs.getInt("advanced"))
                                                newStmt.setInt(5, rs.getInt("advanced_a"))

                                                val nickname = rs.getString("nickname") ?: "Satoshi"
                                                newStmt.setString(6, nickname.substring(0, min(8, nickname.length)))
                                                newStmt.setInt(7, rs.getInt("badges"))
                                                newStmt.setString(8, rs.getString("avatar") ?: "none")
                                                newStmt.setInt(9, rs.getInt("classic"))
                                                newStmt.setInt(10, rs.getInt("challenge"))
                                                newStmt.setLong(11, rs.getLong("money"))
                                                newStmt.setInt(12, rs.getInt("npcTrade"))
                                                newStmt.setInt(13, rs.getInt("version"))
                                                newStmt.setTimestamp(
                                                    14,
                                                    rs.getTimestamp("created_at")
                                                        ?: Timestamp(Instant.now().epochSecond)
                                                )
                                                newStmt.setTimestamp(15, rs.getTimestamp("updated_at"))
                                                currentRow += newStmt.executeUpdate()
                                            }
                                        }
                                    }

                                    savesPopulatedPerUser.filter { save -> save.value.size < 3 }.forEach { save ->
                                        val numbers = arrayListOf(0, 1, 2)

                                        save.value.forEach { usedNumber -> numbers.remove(usedNumber) }

                                        numbers.forEach { number ->
                                            newStmt.setLong(1, saveId++)
                                            newStmt.setLong(2, save.key)
                                            newStmt.setInt(3, number)
                                            newStmt.setInt(4, 0)
                                            newStmt.setInt(5, 0)

                                            newStmt.setString(6, "Satoshi")
                                            newStmt.setInt(7, 0)

                                            newStmt.setString(8, "none")
                                            newStmt.setInt(9, 0)
                                            newStmt.setInt(10, 0)
                                            newStmt.setLong(11, 50)
                                            newStmt.setInt(12, 0)
                                            newStmt.setInt(13, 1)
                                            newStmt.setTimestamp(14, Timestamp(Instant.now().epochSecond))
                                            newStmt.setTimestamp(15, null)
                                        }
                                    }
                                }
                            }

                        oldConn.prepareStatement(
                            "SELECT save_id, item, COUNT(item) FROM save_items" +
                                    " GROUP BY save_id, item ORDER BY save_id LIMIT $chunking OFFSET ?"
                        ).use { oldStmt ->
                            newConn.prepareStatement("INSERT INTO ptd1_save_items(save_id, item, quantity) VALUES (?, ?, ?)")
                                .use { newStmt ->
                                    for (i in 0..tables["save_items"]!! step chunking) {
                                        if (cancelingMigration) {
                                            throw Exception("Migration canceled")
                                        }

                                        saveItemsOffset = i
                                        oldStmt.setInt(1, i)
                                        oldStmt.executeQuery().use { rs ->
                                            while (rs.next()) {
                                                newStmt.setLong(1, rs.getLong("save_id"))
                                                newStmt.setInt(2, rs.getInt("item"))
                                                newStmt.setInt(3, min(255, rs.getInt("COUNT(item)")))
                                                currentRow += newStmt.executeUpdate()
                                            }
                                        }
                                    }
                                }
                        }

                        oldConn.prepareStatement("SELECT * FROM pokemon WHERE id >= ? ORDER BY id LIMIT $chunking")
                            .use { oldStmt ->
                                newConn.prepareStatement(
                                    "INSERT INTO ptd1_pokemon(id, save_id, swf_id, number, nickname," +
                                            " experience, level, move_1, move_2, move_3, move_4, move_selected, ability, target_type, tag, position," +
                                            " rarity, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                ).use { newStmt ->
                                    for (i in 0..tables["pokemon"]!! step chunking) {
                                        if (cancelingMigration) {
                                            throw Exception("Migration canceled")
                                        }

                                        oldStmt.setLong(1, pokemonId + 1)
                                        oldStmt.executeQuery().use { rs ->
                                            while (rs.next()) {
                                                val id = rs.getLong("id")
                                                pokemonId = id
                                                newStmt.setLong(1, id)
                                                newStmt.setLong(2, rs.getLong("save_id"))
                                                newStmt.setInt(3, rs.getInt("pId"))

                                                newStmt.setInt(4, min(65535, rs.getInt("pNum")))

                                                val nickname = rs.getString("nickname")
                                                newStmt.setString(
                                                    5,
                                                    nickname.substring(0, min(30, nickname.length))
                                                )
                                                newStmt.setInt(6, rs.getInt("exp"))
                                                newStmt.setInt(7, rs.getInt("lvl"))
                                                newStmt.setInt(8, rs.getInt("m1"))
                                                newStmt.setInt(9, rs.getInt("m2"))
                                                newStmt.setInt(10, rs.getInt("m3"))
                                                newStmt.setInt(11, rs.getInt("m4"))
                                                newStmt.setInt(12, rs.getInt("mSel"))
                                                newStmt.setInt(13, rs.getInt("ability"))
                                                newStmt.setInt(14, rs.getInt("targetType"))

                                                val tag = rs.getString("tag") ?: "h"
                                                newStmt.setString(15, tag)
                                                newStmt.setInt(16, rs.getInt("pos"))
                                                newStmt.setInt(17, rs.getInt("shiny"))
                                                newStmt.setTimestamp(
                                                    18,
                                                    rs.getTimestamp("created_at")
                                                        ?: Timestamp(Instant.now().epochSecond)
                                                )
                                                newStmt.setTimestamp(19, rs.getTimestamp("updated_at"))
                                                currentRow += newStmt.executeUpdate()
                                            }
                                        }
                                    }
                                }
                            }

                        newConn.commit()

                        currentRow = totalRows
                        logger.info("Migration Successful!")
                        migrationAsked()
                        intercept = false
                    } catch (e: Exception) {
                        newConn?.rollback()

                        val rowProgress = "Last User ID: $oldUserId, Total Rows in users: ${tables["users"]}\n" +
                                "Last Achievement ID: $achievementId, Total Rows in achievements: ${tables["achievements"]}\n" +
                                "Last Save ID: $saveId, Total Rows in saves: ${tables["saves"]}\n" +
                                "Last Save_items OFFSET: $saveItemsOffset, Total Rows in save_items: ${tables["save_items"]}\n" +
                                "Last Pokemon ID: $pokemonId, Total Rows in pokemon: ${tables["pokemon"]}\n"

                        migrationError = rowProgress + stackTraceToString(e)
                        logger.error(migrationError)
                    }

                    oldConn?.close()
                    newConn?.close()
                    isMigrating = false
                }.start()
            } catch (e: Exception) {
                oldConn?.close()
                pageForm(call, values, stackTraceToString(e))
                isMigrating = false
                return@onCall
            }
        }
    }
}

suspend fun page(call: ApplicationCall, map: Map<String, Any> = mapOf()) {
    call.respond(ThymeleafContent("databaseMigration/index", map/*, csrfMapOf(call.sessions)*/))
}

suspend fun page(call: ApplicationCall, vararg attributes: Pair<String, Any>) {
    page(call, mapOf(*attributes))
}

suspend fun pageReasons(call: ApplicationCall, vararg reasons: String) {
    page(call, "reasons" to reasons)
}

suspend fun pageForm(call: ApplicationCall, formValues: HashMap<String, String>, vararg attributes: Pair<String, Any>) {
    page(call, *formValues.toList().toTypedArray(), *attributes)
}

suspend fun pageForm(call: ApplicationCall, formValues: HashMap<String, String>, vararg reasons: String) {
    pageForm(call, formValues, "reasons" to reasons)
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