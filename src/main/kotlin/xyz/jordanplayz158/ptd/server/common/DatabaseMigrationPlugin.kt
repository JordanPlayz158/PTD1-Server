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
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
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
    val isMigrating1: Boolean = false,
    @Serializable(with = InetAddressNullableSerializer::class)
    val host1: InetAddress?, // NotNull
    val port1: Int?, // Min(value = 1)
    val name1: String?, // NotBlank
    val username1: String?, // NotBlank
    val password1: String?, // NotBlank

    val isMigrating2: Boolean = false,
    @Serializable(with = InetAddressNullableSerializer::class)
    val host2: InetAddress?, // NotNull
    val port2: Int?, // Min(value = 1)
    val name2: String?, // NotBlank
    val username2: String?, // NotBlank
    val password2: String?, // NotBlank
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
            val (dataIsMigrating1, inetAddress1, port1, name1, username1, password1,
                dataIsMigrating2, inetAddress2, port2, name2, username2, password2) = call.receive<DatabaseMigration>()

            if (!dataIsMigrating1 && !dataIsMigrating2) return@onCall cancel(call)

            if(isMigrating) {
                pageReasons(call, "Migration in progress!")
                return@onCall
            }

            val values = HashMap<String, String>()
            val reasons = ArrayList<String>()

            if(dataIsMigrating1) {
                if (inetAddress1 === null) reasons.add("'host' for PTD1 is not a valid IPv4 or IPv6 address!") else values["host"] =
                    inetAddress1.hostName

                if (port1 === null) reasons.add("'port' for PTD1 is not a valid integer!") else {
                    if (port1 <= 0) reasons.add("'port' for PTD1 is not a valid port, ports cannot be negative or zero!") else values["port"] =
                        "$port1"
                }

                if (name1 === null) reasons.add("'name' for PTD1 is blank!") else values["name"] = name1
                if (username1 === null) reasons.add("'username' for PTD1 is blank!") else values["username"] =
                    username1
                if (password1 === null) reasons.add("'password' for PTD1 is blank!")
            }

            if (dataIsMigrating2) {
                if (inetAddress2 === null) reasons.add("'host' for PTD2 is not a valid IPv4 or IPv6 address!") else values["host"] =
                    inetAddress2.hostName

                if (port2 === null) reasons.add("'port' for PTD2 is not a valid integer!") else {
                    if (port2 <= 0) reasons.add("'port' for PTD2 is not a valid port, ports cannot be negative or zero!") else values["port"] =
                        "$port2"
                }

                if (name2 === null) reasons.add("'name' for PTD2 is blank!") else values["name"] = name2
                if (username2 === null) reasons.add("'username' for PTD2 is blank!") else values["username"] =
                    username2
                if (password2 === null) reasons.add("'password' for PTD2 is blank!")
            }

            if(reasons.isNotEmpty()) {
                pageForm(call, values, *reasons.toTypedArray())
                return@onCall
            }

            logger.info("Migrating...")

            cancelingMigration = false
            isMigrating = true

            var oldConnPtd1: Connection? = null
            var oldConnPtd2: Connection? = null

            var tablesPtd1: MutableMap<String, Int>? = null
            var tablesPtd2: MutableMap<String, Int>? = null
            try {
                if (dataIsMigrating1) {
                    oldConnPtd1 = DriverManager.getConnection(
                        "jdbc:mariadb://${inetAddress1!!.hostName}:$port1/$name1",
                        username1,
                        password1
                    )

                    tablesPtd1 =
                        mutableMapOf(
                            "achievements" to 0,
                            "pokemon" to 0,
                            "saves" to 0,
                            "save_items" to 0,
                            "users" to 0
                        )
                    for ((table, _) in tablesPtd1) {
                        oldConnPtd1.createStatement().use { stmt ->
                            stmt.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                                rs.next()
                                val numberOfRows = rs.getInt(1)

                                totalRows += numberOfRows
                                tablesPtd1[table] = numberOfRows
                            }
                        }
                    }
                }

                if (dataIsMigrating2) {
                    oldConnPtd2 = DriverManager.getConnection(
                        "jdbc:mariadb://${inetAddress2!!.hostName}:$port2/$name2",
                        username2,
                        password2
                    )

                    tablesPtd2 =
                        mutableMapOf(
                            "1v1" to 0,
                            "accounts" to 0,
                            "extra" to 0,
                            "items" to 0,
                            "pokes" to 0,
                            "story" to 0
                        )
                    for ((table, _) in tablesPtd2) {
                        oldConnPtd2.createStatement().use { stmt ->
                            stmt.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                                rs.next()
                                val numberOfRows = rs.getInt(1)

                                totalRows += numberOfRows
                                tablesPtd2[table] = numberOfRows
                            }
                        }
                    }
                }

                pageForm(call, values, "totalRows" to totalRows)

                Thread {
                    var newConn: Connection? = null
                    var oldUserIdPtd1 = 0L
                    var achievementIdPtd1 = 0L
                    var saveIdPtd1 = 0L
                    var saveItemsOffsetPtd1 = 0
                    var pokemonIdPtd1 = 0L

                    // PTD2
                    var oldAccountsOffsetPtd2 = 0
                    var oneV1OffsetPtd2 = 0
                    var extraOffsetPtd2 = 0
                    var itemsOffsetPtd2 = 0
                    var pokesOffsetPtd2 = 0
                    var storyOffsetPtd2 = 0

                    try {
                        newConn = dataSource.connection
                        newConn.autoCommit = false

                        val chunking = 1000

                        if (dataIsMigrating1) {
                            val oldConnPtd1 = oldConnPtd1!!
                            val tablesPtd1 = tablesPtd1!!

                            oldConnPtd1.prepareStatement("SELECT * FROM users WHERE id >= ? ORDER BY id LIMIT $chunking")
                                .use { oldStmt ->
                                    newConn.prepareStatement("INSERT INTO users(email, role_id, password) VALUES (?, ?, ?)")
                                        .use { newStmt ->
                                            newConn.prepareStatement(
                                                "INSERT INTO ptd1_users(id, user_id, dex," +
                                                        " shiny_dex, shadow_dex, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                            ).use { newPtd1Stmt ->
                                                for (i in 0..tablesPtd1["users"]!! step chunking) {
                                                    if (cancelingMigration) {
                                                        throw Exception("Migration canceled")
                                                    }

                                                    oldStmt.setLong(1, oldUserIdPtd1 + 1)
                                                    oldStmt.executeQuery().use { rs ->
                                                        while (rs.next()) {
                                                            newStmt.setString(
                                                                1,
                                                                rs.getString("email")
                                                            )

                                                            val roleId =
                                                                when (rs.getLong("role_id")) {
                                                                    2L -> 1L
                                                                    else -> 100L
                                                                }

                                                            newStmt.setLong(2, roleId)
                                                            newStmt.setString(
                                                                3,
                                                                rs.getString("password")
                                                            )
                                                            newStmt.executeUpdate()

                                                            oldUserIdPtd1 = rs.getLong("id")
                                                            newPtd1Stmt.setLong(1, oldUserIdPtd1)

                                                            newStmt.generatedKeys.next()

                                                            newPtd1Stmt.setLong(
                                                                2,
                                                                newStmt.generatedKeys.getLong(1)
                                                            )
                                                            newPtd1Stmt.setString(
                                                                3,
                                                                rs.getString("dex") ?: ""
                                                            )
                                                            newPtd1Stmt.setString(
                                                                4,
                                                                rs.getString("shinyDex") ?: ""
                                                            )
                                                            newPtd1Stmt.setString(
                                                                5,
                                                                rs.getString("shadowDex") ?: ""
                                                            )
                                                            newPtd1Stmt.setTimestamp(
                                                                6,
                                                                rs.getTimestamp("created_at")
                                                            )
                                                            newPtd1Stmt.setTimestamp(
                                                                7,
                                                                rs.getTimestamp("updated_at")
                                                            )

                                                            currentRow += newPtd1Stmt.executeUpdate()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                }


                            oldConnPtd1.prepareStatement("SELECT * FROM achievements WHERE id >= ? ORDER BY id LIMIT $chunking")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd1_achievements(id, user_id, shiny_hunter_rattata," +
                                                " shiny_hunter_pidgey, shiny_hunter_geodude, shiny_hunter_zubat, star_wars, no_advantage, win_without_wind," +
                                                " needs_more_candy, the_hard_way, pewter_challenge, cerulean_challenge, vermillion_challenge," +
                                                " celadon_challenge, saffron_city_challenge, fuchsia_gym_challenge, cinnabar_gym_challenge," +
                                                " viridian_city_challenge, created_at, updated_at)" +
                                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd1["achievements"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            oldStmt.setLong(1, achievementIdPtd1 + 1)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val id = rs.getLong("id")
                                                    achievementIdPtd1 = id
                                                    newStmt.setLong(1, id)
                                                    newStmt.setLong(2, rs.getLong("user_id"))

                                                    val shinyHunter =
                                                        rs.getString("one").trim().chunked(1)
                                                            .toMutableList()
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
                                                    newStmt.setTimestamp(
                                                        21,
                                                        rs.getTimestamp("updated_at")
                                                    )
                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                                }


                            val savesPopulatedPerUser = HashMap<Long, ArrayList<Int>>()
                            oldConnPtd1.prepareStatement("SELECT * FROM saves WHERE id >= ? ORDER BY id LIMIT $chunking")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd1_saves(id, user_id, number, levels_completed," +
                                                " levels_started, nickname, badges, avatar, has_flash, challenge, money, npc_trade, version, created_at, updated_at)" +
                                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd1["saves"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled.")
                                            }

                                            oldStmt.setLong(1, saveIdPtd1 + 1)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val id = rs.getLong("id")
                                                    saveIdPtd1 = id
                                                    newStmt.setLong(1, id)

                                                    val oldUserId = rs.getLong("user_id")
                                                    newStmt.setLong(2, oldUserId)

                                                    val number = rs.getInt("num")
                                                    newStmt.setInt(3, number)

                                                    savesPopulatedPerUser.putIfAbsent(
                                                        oldUserId,
                                                        ArrayList()
                                                    )
                                                    savesPopulatedPerUser[oldUserId]?.add(number)

                                                    newStmt.setInt(4, rs.getInt("advanced"))
                                                    newStmt.setInt(5, rs.getInt("advanced_a"))

                                                    val nickname =
                                                        rs.getString("nickname") ?: "Satoshi"
                                                    newStmt.setString(
                                                        6,
                                                        nickname.substring(
                                                            0,
                                                            min(8, nickname.length)
                                                        )
                                                    )
                                                    newStmt.setInt(7, rs.getInt("badges"))
                                                    newStmt.setString(
                                                        8,
                                                        rs.getString("avatar") ?: "none"
                                                    )
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
                                                    newStmt.setTimestamp(
                                                        15,
                                                        rs.getTimestamp("updated_at")
                                                    )
                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }

                                        savesPopulatedPerUser.filter { save -> save.value.size < 3 }
                                            .forEach { save ->
                                                val numbers = arrayListOf(0, 1, 2)

                                                save.value.forEach { usedNumber ->
                                                    numbers.remove(
                                                        usedNumber
                                                    )
                                                }

                                                numbers.forEach { number ->
                                                    newStmt.setLong(1, saveIdPtd1++)
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
                                                    newStmt.setTimestamp(
                                                        14,
                                                        Timestamp(Instant.now().epochSecond)
                                                    )
                                                    newStmt.setTimestamp(15, null)
                                                }
                                            }
                                    }
                                }

                            oldConnPtd1.prepareStatement(
                                "SELECT save_id, item, COUNT(item) FROM save_items" +
                                        " GROUP BY save_id, item ORDER BY save_id LIMIT $chunking OFFSET ?"
                            ).use { oldStmt ->
                                newConn.prepareStatement("INSERT INTO ptd1_save_items(save_id, item, quantity) VALUES (?, ?, ?)")
                                    .use { newStmt ->
                                        for (i in 0..tablesPtd1["save_items"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            saveItemsOffsetPtd1 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    newStmt.setLong(1, rs.getLong("save_id"))
                                                    newStmt.setInt(2, rs.getInt("item"))
                                                    newStmt.setInt(
                                                        3,
                                                        min(255, rs.getInt("COUNT(item)"))
                                                    )
                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                            }

                            oldConnPtd1.prepareStatement("SELECT * FROM pokemon WHERE id >= ? ORDER BY id LIMIT $chunking")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd1_pokemon(id, save_id, swf_id, number, nickname," +
                                                " experience, level, move_1, move_2, move_3, move_4, move_selected, ability, target_type, tag, position," +
                                                " rarity, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd1["pokemon"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            oldStmt.setLong(1, pokemonIdPtd1 + 1)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val id = rs.getLong("id")
                                                    pokemonIdPtd1 = id
                                                    newStmt.setLong(1, id)
                                                    newStmt.setLong(2, rs.getLong("save_id"))
                                                    newStmt.setInt(3, rs.getInt("pId"))

                                                    newStmt.setInt(4, min(65535, rs.getInt("pNum")))

                                                    val nickname = rs.getString("nickname")
                                                    newStmt.setString(
                                                        5,
                                                        nickname.substring(
                                                            0,
                                                            min(30, nickname.length)
                                                        )
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
                                                    newStmt.setTimestamp(
                                                        19,
                                                        rs.getTimestamp("updated_at")
                                                    )
                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        // PTD2

                        if (dataIsMigrating2) {
                            val oldConnPtd2 = oldConnPtd2!!
                            val tablesPtd2 = tablesPtd2!!

                            val emailToId: HashMap<String, Long> = hashMapOf()

                            oldConnPtd2.prepareStatement("SELECT * FROM accounts ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement("INSERT INTO users(email, role_id, password) VALUES (?, ?, ?)")
                                        .use { newStmt ->
                                            newConn.prepareStatement(
                                                "INSERT INTO ptd2_users(user_id, gen_1_dex," +
                                                        " gen_2_dex, gen_3_dex, gen_4_dex, gen_5_dex, gen_6_dex) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                            ).use { newPtd2Stmt ->
                                                for (i in 0..tablesPtd2["accounts"]!! step chunking) {
                                                    if (cancelingMigration) {
                                                        throw Exception("Migration canceled")
                                                    }

                                                    oldAccountsOffsetPtd2 = i
                                                    oldStmt.setInt(1, i)
                                                    oldStmt.executeQuery().use { rs ->
                                                        while (rs.next()) {
                                                            val email = rs.getString("email")
                                                            newStmt.setString(
                                                                1,
                                                                "ptd2_$email"
                                                            )

                                                            newStmt.setLong(2, 100L)
                                                            newStmt.setString(
                                                                3,
                                                                rs.getString("pass")
                                                            )
                                                            newStmt.executeUpdate()


                                                            newStmt.generatedKeys.next()

                                                            newPtd2Stmt.setLong(
                                                                1,
                                                                newStmt.generatedKeys.getLong(1)
                                                            )
                                                            newPtd2Stmt.setString(
                                                                2,
                                                                rs.getString("dex1") ?: ""
                                                            )
                                                            newPtd2Stmt.setString(
                                                                3,
                                                                rs.getString("dex2") ?: ""
                                                            )
                                                            newPtd2Stmt.setString(
                                                                4,
                                                                rs.getString("dex3") ?: ""
                                                            )
                                                            newPtd2Stmt.setString(
                                                                5,
                                                                rs.getString("dex4") ?: ""
                                                            )
                                                            newPtd2Stmt.setString(
                                                                6,
                                                                rs.getString("dex5") ?: ""
                                                            )
                                                            newPtd2Stmt.setString(
                                                                7,
                                                                rs.getString("dex6") ?: ""
                                                            )

                                                            currentRow += newPtd2Stmt.executeUpdate()

                                                            newPtd2Stmt.generatedKeys.next()
                                                            emailToId[email] =
                                                                newPtd2Stmt.generatedKeys.getLong(1)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                }

                            oldConnPtd2.prepareStatement("SELECT * FROM 1v1 ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO `ptd2_1v1s`(user_id," +
                                                " number, money, levels_unlocked) VALUES (?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd2["1v1"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            oneV1OffsetPtd2 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val email = rs.getString("email")

                                                    var id = emailToId[email]

                                                    // Fix because some (or maybe just 1)
                                                    //  entries somehow have email with a leading
                                                    //  or trailing space while the email in accounts does not
                                                    if (id === null) {
                                                        for ((key, value) in emailToId.entries) {
                                                            if (key.trim() == email.trim()) id = value
                                                        }
                                                    }

                                                    newStmt.setLong(
                                                        1,
                                                        id!!
                                                    )

                                                    val number = rs.getByte("num")

                                                    newStmt.setByte(
                                                        2,
                                                        number
                                                    )

                                                    val money = rs.getInt("money")

                                                    newStmt.setInt(
                                                        3,
                                                        money
                                                    )

                                                    val levelsUnlocked = rs.getByte("levelUnlocked")

                                                    newStmt.setByte(
                                                        4,
                                                        levelsUnlocked
                                                    )

                                                    try {
                                                        currentRow += newStmt.executeUpdate()
                                                    } catch (e: SQLException) {
                                                        // If unique constraint fails
                                                        //  skip over
                                                        if (e.errorCode == 19) {
                                                            currentRow++
                                                            continue
                                                        }

                                                        throw e
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            val emailAndNumberToStoryId: HashMap<Pair<String, Byte>, Long> = hashMapOf()

                            oldConnPtd2.prepareStatement("SELECT * FROM story ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd2_saves(user_id," +
                                                " number, current_map, map_spot, nickname, gender, money, current_time, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd2["story"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            storyOffsetPtd2 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val email = rs.getString("email")
                                                    val number = rs.getByte("num")

                                                    var id = emailToId[email]

                                                    // Need to do same for story
                                                    if (id === null) {
                                                        for ((key, value) in emailToId.entries) {
                                                            if (key.trim() == email.trim()) id = value
                                                        }
                                                    }

                                                    newStmt.setLong(
                                                        1,
                                                        id!!
                                                    )
                                                    newStmt.setByte(
                                                        2,
                                                        number
                                                    )
                                                    newStmt.setByte(
                                                        3,
                                                        rs.getByte("MapLoc")
                                                    )
                                                    newStmt.setByte(
                                                        4,
                                                        rs.getByte("MapSpot")
                                                    )

                                                    newStmt.setString(
                                                        5,
                                                        rs.getString("Nickname")
                                                    )

                                                    newStmt.setByte(
                                                        6,
                                                        rs.getByte("Gender")
                                                    )

                                                    newStmt.setInt(
                                                        7,
                                                        rs.getInt("Money")
                                                    )

                                                    newStmt.setShort(
                                                        8,
                                                        rs.getShort("CurrentTime")
                                                    )

                                                    newStmt.setByte(
                                                        9,
                                                        rs.getByte("Color")
                                                    )

                                                    try {
                                                        currentRow += newStmt.executeUpdate()

                                                        newStmt.generatedKeys.next()
                                                        emailAndNumberToStoryId[email to number] =
                                                            newStmt.generatedKeys.getLong(1)
                                                    } catch (e: SQLException) {
                                                        // If unique constraint fails
                                                        //  skip over, confirmed all current
                                                        //  record duplicates are populated with the same data
                                                        if (e.errorCode == 19) {
                                                            currentRow++
                                                            continue
                                                        }

                                                        throw e
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            oldConnPtd2.prepareStatement("SELECT * FROM extra ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd2_extras(save_id," +
                                                " is_item, number, value) VALUES (?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd2["extra"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            extraOffsetPtd2 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val email = rs.getString("email")
                                                    val owner = rs.getByte("owner")

                                                    var id = emailAndNumberToStoryId[email to owner]
                                                    if (id === null) {
                                                        for ((key, value) in emailAndNumberToStoryId.entries) {
                                                            if (key.first.trim() == email.trim()
                                                                && key.second == owner) id = value
                                                        }
                                                    }

                                                    // If id is still null then, somehow the user removed the story but not the extras
                                                    //   maybe extras aren't cleared when story is?
                                                    if (id === null) {
                                                        currentRow++
                                                        continue
                                                    }

                                                    newStmt.setLong(
                                                        1,
                                                        id!!
                                                    )
                                                    newStmt.setBoolean(
                                                        2,
                                                        false
                                                    )
                                                    newStmt.setShort(
                                                        3,
                                                        rs.getShort("num")
                                                    )
                                                    newStmt.setInt(
                                                        4,
                                                        rs.getInt("value")
                                                    )

                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                                }

                            oldConnPtd2.prepareStatement("SELECT * FROM items ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd2_extras(save_id," +
                                                " is_item, number, value) VALUES (?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd2["items"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            itemsOffsetPtd2 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val email = rs.getString("email")
                                                    val owner = rs.getByte("owner")

                                                    var id = emailAndNumberToStoryId[email to owner]
                                                    if (id === null) {
                                                        for ((key, value) in emailAndNumberToStoryId.entries) {
                                                            if (key.first.trim() == email.trim()
                                                                && key.second == owner) id = value
                                                        }
                                                    }

                                                    if (id === null) {
                                                        currentRow++
                                                        continue
                                                    }

                                                    newStmt.setLong(
                                                        1,
                                                        id!!
                                                    )
                                                    newStmt.setBoolean(
                                                        2,
                                                        true
                                                    )
                                                    newStmt.setShort(
                                                        3,
                                                        rs.getShort("num")
                                                    )
                                                    newStmt.setInt(
                                                        4,
                                                        rs.getInt("value")
                                                    )

                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                                }

                            oldConnPtd2.prepareStatement("SELECT * FROM pokes ORDER BY email LIMIT $chunking OFFSET ?")
                                .use { oldStmt ->
                                    newConn.prepareStatement(
                                        "INSERT INTO ptd2_pokemon(save_id," +
                                                " swf_id, number, nickname, experience, level, move_1, move_2, move_3, move_4, target_type, tag, position, gender, extra, item) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                    ).use { newStmt ->
                                        for (i in 0..tablesPtd2["pokes"]!! step chunking) {
                                            if (cancelingMigration) {
                                                throw Exception("Migration canceled")
                                            }

                                            pokesOffsetPtd2 = i
                                            oldStmt.setInt(1, i)
                                            oldStmt.executeQuery().use { rs ->
                                                while (rs.next()) {
                                                    val email = rs.getString("email")
                                                    val owner = rs.getByte("owner")

                                                    var saveId = emailAndNumberToStoryId[email to owner]

                                                    if (saveId === null) {
                                                        for ((key, value) in emailAndNumberToStoryId.entries) {
                                                            if (key.first.trim() == email.trim()
                                                                && key.second == owner) saveId = value
                                                        }
                                                    }

                                                    if (saveId === null) {
                                                        currentRow++
                                                        continue
                                                    }

                                                    newStmt.setLong(1, saveId)

                                                    newConn.prepareStatement("SELECT swf_id FROM ptd2_pokemon WHERE save_id = ? ORDER BY swf_id DESC LIMIT 1")
                                                        .use { lastSwfIdStmt ->
                                                            lastSwfIdStmt.setLong(1, saveId)
                                                            val swfIdRS = lastSwfIdStmt.executeQuery()


                                                            newStmt.setLong(2, if (swfIdRS.next()) {
                                                                swfIdRS.getLong(1) + 1
                                                            } else {
                                                                1
                                                            })
                                                    }

                                                    newStmt.setShort(3, rs.getShort("num"))
                                                    newStmt.setString(4, rs.getString("Nickname"))
                                                    newStmt.setInt(5, rs.getInt("xp"))
                                                    newStmt.setByte(6, rs.getByte("lvl"))
                                                    newStmt.setShort(7, rs.getShort("move1"))
                                                    newStmt.setShort(8, rs.getShort("move2"))
                                                    newStmt.setShort(9, rs.getShort("move3"))
                                                    newStmt.setShort(10, rs.getShort("move4"))
                                                    newStmt.setByte(11, rs.getByte("targetingType"))
                                                    newStmt.setString(12, rs.getString("tag"))
                                                    newStmt.setInt(13, rs.getInt("pos"))
                                                    newStmt.setByte(14, rs.getByte("gender"))
                                                    newStmt.setByte(15, rs.getByte("extra"))
                                                    newStmt.setByte(16, rs.getByte("item"))

                                                    currentRow += newStmt.executeUpdate()
                                                }
                                            }
                                        }
                                    }
                                }
                        }



                        // Complete
                        newConn.commit()

                        currentRow = totalRows
                        logger.info("Migration Successful!")
                        migrationAsked()
                        intercept = false
                    } catch (e: Exception) {
                        newConn?.rollback()

                        val rowProgress = StringBuilder()

                        if (dataIsMigrating1) {
                            val tablesPtd1 = tablesPtd1!!

                            rowProgress.append(
                                "PTD1 Rows\n" +
                                        "Last User ID: $oldUserIdPtd1, Total Rows in users: ${tablesPtd1["users"]}\n" +
                                        "Last Achievement ID: $achievementIdPtd1, Total Rows in achievements: ${tablesPtd1["achievements"]}\n" +
                                        "Last Save ID: $saveIdPtd1, Total Rows in saves: ${tablesPtd1["saves"]}\n" +
                                        "Last Save_items OFFSET: $saveItemsOffsetPtd1, Total Rows in save_items: ${tablesPtd1["save_items"]}\n" +
                                        "Last Pokemon ID: $pokemonIdPtd1, Total Rows in pokemon: ${tablesPtd1["pokemon"]}\n")
                        }

                        if (dataIsMigrating2) {
                            val tablesPtd2 = tablesPtd2!!
                            rowProgress.append(
                                "PTD2 Rows\n" +
                                        "Last Account OFFSET: $oldAccountsOffsetPtd2, Total Rows in accounts: ${tablesPtd2["accounts"]}\n" +
                                        "Last 1v1 OFFSET: $oneV1OffsetPtd2, Total Rows in 1v1: ${tablesPtd2["1v1"]}\n" +
                                        "Last Story OFFSET: $storyOffsetPtd2, Total Rows in story: ${tablesPtd2["story"]}\n" +
                                        "Last Extra OFFSET: $extraOffsetPtd2, Total Rows in extras: ${tablesPtd2["extra"]}\n" +
                                        "Last Item OFFSET: $itemsOffsetPtd2, Total Rows in items: ${tablesPtd2["items"]}\n" +
                                        "Last Pokes OFFSET: $pokesOffsetPtd2, Total Rows in pokes: ${tablesPtd2["pokes"]}\n")
                        }

                        migrationError = rowProgress.toString() + stackTraceToString(e)
                        logger.error(migrationError)
                    }

                    oldConnPtd1?.close()
                    oldConnPtd2?.close()
                    newConn?.close()
                    isMigrating = false
                }.start()
            } catch (e: Exception) {
                oldConnPtd1?.close()
                oldConnPtd2?.close()
                pageForm(call, values, stackTraceToString(e))
                isMigrating = false
                return@onCall
            }
        }
    }
}

suspend fun page(call: ApplicationCall, map: Map<String, Any> = mapOf()) {
    call.respond(ThymeleafContent("databaseMigration/2column", map/*, csrfMapOf(call.sessions)*/))
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