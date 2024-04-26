package xyz.jordanplayz158.ptd.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.webjars.*
import me.nathanfallet.ktorx.plugins.KtorSentry
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.thymeleaf.context.IExpressionContext
import org.thymeleaf.context.IWebContext
import org.thymeleaf.linkbuilder.StandardLinkBuilder
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import xyz.jordanplayz158.ptd.server.common.FirstRunDatabaseMigrationPlugin
import xyz.jordanplayz158.ptd.server.common.data.CustomFormUrlEncodedConverter
import xyz.jordanplayz158.ptd.server.common.orm.Setting
import xyz.jordanplayz158.ptd.server.common.orm.Settings
import xyz.jordanplayz158.ptd.server.common.session.SQLSessionStorage
import xyz.jordanplayz158.ptd.server.common.session.UserSession
import xyz.jordanplayz158.ptd.server.migration.SQLMigration
import xyz.jordanplayz158.ptd.server.module.ptd1.ptd1
import xyz.jordanplayz158.ptd.server.module.ptd2.ptd2
import xyz.jordanplayz158.ptd.server.module.ptd3.ptd3
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

val dotenv = dotenv()
lateinit var dataSource: HikariDataSource

fun main() {
    // `db`, `static`, `templates`, `.env` will be shipped in a zip, no need to copy at runtime
    //copyResourceToFileSystem(".env", File(".env"))

    dataSource = HikariDataSource(databaseConfig(dotenv))
    val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)

    embeddedServer(CIO, port = dotenv["PORT", "8080"].toInt()) {
        val sentryUrl = dotenv["SENTRY_URL"]
        if (sentryUrl !== null) {
            install(KtorSentry) {
                dsn = sentryUrl
                tracesSampleRate = dotenv["SENTRY_SAMPLE_RATE", "1.0"].toDouble()
                isDebug = dotenv["SENTRY_DEBUG", "false"].toBoolean()
            }
        }

        SQLMigration(
            dataSource.connection,
            getCorrectFile("db/migration/ptd/$databaseServer", developmentMode)
        )

        val database = Database.connect(dataSource)

        install(ContentNegotiation) {
            register(ContentType.Application.FormUrlEncoded, CustomFormUrlEncodedConverter())
        }

        // TODO: Need to ensure only first migration is run if DB_MIGRATION_ASKED is not yes so schema will be correct for
        //  migrating if the user wishes to migrate, currently it will just go through all the migrations in the directory
        // For the "first run" (if the user hasn't answered yes or no) of this server
        //   We will display 1 page for all routes to migrate old DB data if requested.
        transaction {
            val dbMigrationAsked = Setting.find(Settings.key eq "DB_MIGRATION_ASKED").firstOrNull()
            if (dbMigrationAsked === null) {
                install(FirstRunDatabaseMigrationPlugin)
            }
        }

        install(Webjars) {
            path = "assets/webjars"
        }

        install(Sessions) {
            cookie<UserSession>("ptd1_session", SQLSessionStorage())
        }

        install(Thymeleaf) {
            addDialect(LayoutDialect())

            setLinkBuilder(object : StandardLinkBuilder() {
                override fun computeContextPath(
                    context: IExpressionContext?,
                    base: String?,
                    parameters: MutableMap<String, Any>?
                ): String {
                    if (context is IWebContext) {
                        return super.computeContextPath(context, base, parameters)
                    }

                    if (base != null && base.startsWith("/webjars/")) {
                        return "/assets"
                    }

                    return ""
                }
            })

            setTemplateResolver((if (developmentMode) {
                ClassLoaderTemplateResolver().apply {
                    cacheManager = null
                }
            } else {
                FileTemplateResolver()
            }).apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "utf-8"
            })
        }

        routing {
            staticFiles("/", getCorrectFile("static", developmentMode))

            get("/") { call.respond(ThymeleafContent("index", mapOf())) }

            get("/flash") {
                // Default invalid character
                val game = call.parameters["game"] ?: "/"

                val reasons = ArrayList<String>()
                if (!game.matches("[A-z0-9-.]+".toRegex())) {
                    reasons.add("Invalid 'game' query parameter")
                }

                call.respond(ThymeleafContent("flash", mapOf("reasons" to reasons, "game" to game)))
            }
        }

        if (dotenv["ENABLE_PTD1", "false"].toBoolean()) {
            SQLMigration(
                dataSource.connection,
                getCorrectFile("db/migration/ptd1/$databaseServer", developmentMode)
            )
            ptd1()
        }

        if (dotenv["ENABLE_PTD2", "false"].toBoolean()) {
            SQLMigration(
                dataSource.connection,
                getCorrectFile("db/migration/ptd2/$databaseServer", developmentMode)
            )
            ptd2()
        }

        if (dotenv["ENABLE_PTD3", "false"].toBoolean()) {
            SQLMigration(
                dataSource.connection,
                getCorrectFile("db/migration/ptd3/$databaseServer", developmentMode)
            )
            ptd3()
        }

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            println("Shutdown signal received. Exiting...")
            TransactionManager.closeAndUnregister(database)
            dataSource.close()
        })
    }.start(wait = true)
}

fun databaseConfig(dotenv: Dotenv) : HikariConfig {
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

    return config
}

fun getCorrectFile(path: String, developmentMode: Boolean) : File {
    return when (developmentMode) {
        // For development, it is nicer to bypass the folders and allow changes to
        //  resources folder directly
        true -> File(object {}.javaClass.getResource("/$path")!!.toURI())
        false -> File(path)
    }
}