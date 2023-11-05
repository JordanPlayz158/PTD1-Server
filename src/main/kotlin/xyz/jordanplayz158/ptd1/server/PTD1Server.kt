package xyz.jordanplayz158.ptd1.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import freemarker.cache.FileTemplateLoader
import freemarker.core.HTMLOutputFormat
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.ContentType
import io.ktor.http.formUrlEncode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.freemarker.FreeMarker
import io.ktor.server.http.content.staticFiles
import io.ktor.server.jetty.EngineMain
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.webjars.Webjars
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.ResourceProvider
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.internal.scanner.LocationScannerCache
import org.flywaydb.core.internal.scanner.ResourceNameCache
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd1.server.controller.SWFController
import xyz.jordanplayz158.ptd1.server.orm.Setting
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.Locale
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.random.Random

object Roles : LongIdTable("roles") {
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now()).nullable()
}

object Users : LongIdTable("users") {
    val email = varchar("email", 50).uniqueIndex()
    val role = reference("role_id", Roles)
    val password = varchar("password", 72)
    val dex = varchar("dex", 151).default("")
    val shinyDex = varchar("shiny_dex", 151).default("")
    val shadowDex = varchar("shadow_dex", 151).default("")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object Saves : LongIdTable("saves") {
    val user = reference("user_id", Users)
    val number = short("number")
    val levelsCompleted = short("levels_completed").default(0)
    val levelsStarted = short("levels_started").default(0)
    val nickname = varchar("nickname", 8).default("Satoshi")
    val badges = short("badges").default(0)
    val avatar = varchar("avatar", 4).default("none")
    val hasFlash = bool("has_flash").default(false)
    val challenge = short("challenge").default(0)
    val money = integer("money").default(50)
    val npcTrade = bool("npc_trade").default(false)
    val version = short("version")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object SaveItems : LongIdTable("save_items") {
    val save = reference("save_id", Saves)
    val item = short("item").uniqueIndex()
    val count = short("count").default(1)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

// I am pure evil
object Pokemons : LongIdTable("pokemon") {
    val save = reference("save_id", Saves)
    // Remember this is auto_increment in code would
    // mark as such but from the sounds of it exposed
    // may try to make it auto_increment in the database
    val swfId = integer("swf_id")
    val number = short("number")
    val nickname = varchar("nickname", 30)
    val experience = integer("experience").default(0)
    val level = short("level").default(1)
    val move1 = short("move_1").default(1)
    val move2 = short("move_2").default(0)
    val move3 = short("move_3").default(0)
    val move4 = short("move_4").default(0)
    val moveSelected = short("move_selected").default(1)
    val ability = short("ability").default(0)
    val targetType = short("target_type").default(1)
    val tag = varchar("tag", 3).default("h")
    val position = integer("position").default(1)
    val rarity = short("rarity").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()

    init {
        index(true, save, swfId)
    }
}

object Achievements : LongIdTable("achievements") {
    val user = reference("user_id", Users)
    val shinyHunterRattata = bool("shiny_hunter_rattata").default(false)
    val shinyHunterPidgey = bool("shiny_hunter_pidgey").default(false)
    val shinyHunterGeodude = bool("shiny_hunter_geodude").default(false)
    val shinyHunterZubat = bool("shiny_hunter_zubat").default(false)
    val starWars = bool("star_wars").default(false)
    val noAdvantage = bool("no_advantage").default(false)
    val winWithoutWind = bool("win_without_wind").default(false)
    val needsMoreCandy = bool("needs_more_candy").default(false)
    val theHardWay = bool("the_hard_way").default(false)
    val pewterChallenge = bool("pewter_challenge").default(false)
    val ceruleanChallenge = bool("cerulean_challenge").default(false)
    val vermillionChallenge = bool("vermillion_challenge").default(false)
    val celadonChallenge = bool("celadon_challenge").default(false)
    val saffronCityChallenge = bool("saffron_city_challenge").default(false)
    val fuchsiaGymChallenge = bool("fuchsia_gym_challenge").default(false)
    val cinnabarGymChallenge = bool("cinnabar_gym_challenge").default(false)
    val viridianCityChallenge = bool("viridian_city_challenge").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object AchievementRedemptions : LongIdTable("achievement_redemptions") {
    val achievement = reference("achievement_id", Achievements)
    val completions = short("completions").default(0)
    val redemptions = short("redemptions").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object Settings : LongIdTable("settings") {
    val key = varchar("key", 255).uniqueIndex()
    val value = varchar("value", 255)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}


fun main(args: Array<String>) = EngineMain.main(args)

var dataSource: HikariDataSource? = null

fun Application.module() {
    copyResourceToFileSystem(".env", File(".env"))

    val dotenv = dotenv()
    val config = HikariConfig()

    config.jdbcUrl = dotenv["DATABASE_URL"]
    config.username = dotenv["DATABASE_USERNAME"]
    config.password = dotenv["DATABASE_PASSWORD"]

    val databaseDriver = dotenv["DATABASE_DRIVER"]
    if(databaseDriver !== null) config.driverClassName = databaseDriver

    config.addDataSourceProperty("cachePrepStmts", dotenv["DATABASE_CACHE_PREP_STMTS", "true"])
    config.addDataSourceProperty("prepStmtCacheSize", dotenv["DATABASE_PREP_STMT_CACHE_SIZE", "375"])
    config.addDataSourceProperty("prepStmtCacheSqlLimit", dotenv["DATABASE_PREP_STMT_CACHE_SQL_LIMIT", "2048"])
    config.addDataSourceProperty("useServerPrepStmts", dotenv["DATABASE_USE_SERVER_PREP_STMTS", "true"])

    dataSource = HikariDataSource(config)

    // Native Image is not supported for Flyway (hopefully) yet
    // https://github.com/flyway/flyway/issues/2927
    // https://github.com/spring-projects/spring-boot/issues/31999
    Flyway.configure().dataSource(dataSource)
        .locations("classpath:db/migration/" + dataSource!!.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH))
        .load().migrate()

    Database.connect(dataSource!!)

    //database.usersQueries.insert("admin", BCrypt.withDefaults().hashToString(12, password.toCharArray()))

    // For the "first run" (if the user hasn't answered yes or no) of this server
    //   We will display 1 page for all routes to migrate old DB data if requested.
    transaction {
        val dbMigrationAsked = Setting.find(Settings.key eq "DB_MIGRATION_ASKED").firstOrNull()
        if (dbMigrationAsked === null || dbMigrationAsked.value == "FALSE") {
            install(FirstRunDatabaseMigrationPlugin)
        }
    }

    install(Webjars) {
        path = "assets"
    }

    // Copying files to filesystem in case the user wants
    // to modify them
    var staticDirectory = File("static")
    var templatesDirectory = File("templates")
    if(!developmentMode) {
        copyResourceRecursivelyToFileSystem(staticDirectory.path)
        copyResourceRecursivelyToFileSystem(templatesDirectory.path)
    } else {
        // For development, it is nicer to bypass the folders and allow changes to
        //  resources folder directly
        staticDirectory = File(object {}.javaClass.getResource("/static")!!.toURI())
        templatesDirectory = File(object {}.javaClass.getResource("/templates")!!.toURI())

    }

    install(FreeMarker) {
        templateLoader = FileTemplateLoader(templatesDirectory)
        outputFormat = HTMLOutputFormat.INSTANCE
    }

    routing {
        staticFiles("/", staticDirectory)

        route("/php") {
            post("/newPoke8.php") {
                val parameters = call.receiveParameters()

                val action = parameters["Action"]
                val email = parameters["Email"]
                val password = parameters["Pass"]
                val ver = parameters["Ver"]
                if (action == null || email == null || password == null || ver == null) {
                    call.respondText(listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to "NotAllParametersSupplied").formUrlEncode(), ContentType.Application.FormUrlEncoded)
                    return@post
                }

                when (action) {
                    "createAccount" -> call.respondText(SWFController.createAccount(email, password).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                    "loadAccount" -> call.respondText(SWFController.loadAccount(email, password).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                    "saveAccount" -> call.respondText(SWFController.saveAccount(call.application.log, email, password, parameters).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                }
            }
        }

//        route("login") {
//            get {
//                call.principal<UserSession>()
//
//                call.respondFile(File(staticDirectory, "login.html"))
//            }
//
//            post {
//                val login = call.receive<LoginRequest>()
//
//                val user = database.usersQueries.selectByUsername(login.username).executeAsOneOrNull()
//
//                if(user !== null && BCrypt.verifyer().verify(login.password.toCharArray(), user.password).verified) {
//                    call.sessions.set(UserSession(id = user.id))
//                    call.respondRedirect("/user")
//                }
//
//                call.respondText("Incorrect credentials.")
//            }
//        }


//        authenticate("auth-session") {
//            get("/user") {
//                call.respond(FreeMarkerContent("user.ftl", mapOf("user" to call.principal<UserSession>())))
//            }
//        }

//        route("api") {
//            post("10") {
//                val request = call.request;
//
//                call.respondText("hi")
//            }
//
//            post("27") {
//
//            }
//        }
    }
}

/**
 * @return true for copying file (file did not exist on filesystem),
 * false for file existed
 */
fun copyResourceToFileSystem(resourcePath: String, fileSystemFile: File) : Boolean {
    val resourceFile = object {}.javaClass.getResourceAsStream("/$resourcePath")

    if(!fileSystemFile.exists()) {
        // Done this way as graalvm doesn't like resources being wrapped in File class
        // Exception in thread "main" java.lang.IllegalArgumentException: URI scheme is not "file"
        resourceFile?.use {input ->
            fileSystemFile.outputStream().use {output ->
                input.copyTo(output)
            }
        }
        return true
    }

    return false
}

fun copyResourceRecursivelyToFileSystem(directory: String) {
    val pathString = object {}.javaClass.getResource("/$directory")?.path

    if(pathString === null) {
        println("Unable to find resource in path '$directory'")
        return
    }

    val resourceDirectory = Path(pathString)
    val fileSystemDirectory = Path(directory)

    Files.walkFileTree(resourceDirectory, object : SimpleFileVisitor<Path>() {
        @Throws(IOException::class)
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.createDirectories(fileSystemDirectory.resolve(resourceDirectory.relativize(dir).toString()))
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val fileSystemFile = fileSystemDirectory.resolve(resourceDirectory.relativize(file).toString())

            if(!fileSystemFile.exists()) {
                Files.copy(file, fileSystemFile, StandardCopyOption.REPLACE_EXISTING)
            }
            return FileVisitResult.CONTINUE
        }
    })
}


val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun randomStringByKotlinRandom(length: Int) = (1..length)
    .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
    .joinToString("")