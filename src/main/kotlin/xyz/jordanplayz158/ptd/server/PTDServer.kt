package xyz.jordanplayz158.ptd.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
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

        // For the "first run" (if the user hasn't answered yes or no) of this server
        //   We will display 1 page for all routes to migrate old DB data if requested.
        transaction {
            val dbMigrationAsked = Setting.find(Settings.key eq "DB_MIGRATION_ASKED").firstOrNull()
            if (dbMigrationAsked === null) {
                install(FirstRunDatabaseMigrationPlugin)
            }
        }

        install(Webjars) {
            path = "assets"
        }

        install(Sessions) {
            cookie<UserSession>("ptd1_session", SQLSessionStorage())
        }

        install(Thymeleaf) {
            addDialect(LayoutDialect())

            setLinkBuilder(
//                object : ILinkBuilder {
//                    override fun getName(): String {
//                        return "dynamicLinks"
//                    }
//
//                    override fun getOrder(): Int {
//                        return 100
//                    }
//
//                    override fun buildLink(
//                        context: IExpressionContext?,
//                        base: String?,
//                        parameters: MutableMap<String, Any>?
//                    ): String {
//                        println()
//                        println("Context: $context")
//                        println("Base: $base")
//                        println("Parameters: $parameters")
//                        println()
//
//                        if (context is IWebContext) {
//                            return s
//                        }
//
//                        return ""
//                    }
//                }

                object : StandardLinkBuilder() {
                    override fun computeContextPath(
                        context: IExpressionContext?,
                        base: String?,
                        parameters: MutableMap<String, Any>?
                    ): String {
                        println()
                        println("Context: $context")
                        println("Base: $base")
                        println("Parameters: $parameters")
                        println()

                        if (context is IWebContext) {
                            return super.computeContextPath(context, base, parameters)

                        }

                        return "static"
                    }
                }
            )

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