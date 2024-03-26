package xyz.jordanplayz158.ptd1.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.ContentType
import io.ktor.http.formUrlEncode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.webjars.Webjars
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
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd.databaseConfig
import xyz.jordanplayz158.ptd.getCorrectFile
import xyz.jordanplayz158.ptd1.server.controller.SWFController
import xyz.jordanplayz158.ptd.migration.SQLMigration
import xyz.jordanplayz158.ptd1.server.orm.Setting
import xyz.jordanplayz158.ptd1.server.session.SQLSessionStorage
import xyz.jordanplayz158.ptd1.server.session.UserSession
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

val dotenv = dotenv()
lateinit var dataSource: HikariDataSource

fun main() {
    // `db`, `static`, `templates`, `.env` will be shipped in a zip, no need to copy at runtime
    //copyResourceToFileSystem(".env", File(".env"))

    dataSource = HikariDataSource(databaseConfig(dotenv))
    val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)

    embeddedServer(CIO, port = dotenv["PORT", "8080"].toInt()) {
        SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd1/$databaseServer", developmentMode))

        val database = Database.connect(dataSource)

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

            route("/php") {
                post("/newPoke8.php") {
                    val parameters = call.receiveParameters()

                    val action = parameters["Action"]
                    val email = parameters["Email"]
                    val password = parameters["Pass"]
                    val ver = parameters["Ver"]
                    if (action == null || email == null || password == null || ver == null) {
                        call.respondText(
                            listOf(
                                "Result" to ResultsEnum.FAILURE.id,
                                "Reason" to "NotAllParametersSupplied"
                            ).formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )
                        return@post
                    }

                    when (action) {
                        "createAccount" -> call.respondText(
                            SWFController.createAccount(
                                email,
                                password
                            ).second.formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )

                        "loadAccount" -> call.respondText(
                            SWFController.loadAccount(
                                email,
                                password
                            ).second.formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )

                        "saveAccount" -> call.respondText(
                            SWFController.saveAccount(
                                call.application.log,
                                email,
                                password,
                                parameters
                            ).second.formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )
                    }
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            println("Shutdown signal received. Exiting...")
            TransactionManager.closeAndUnregister(database)
            dataSource.close()
        })
    }.start(wait = true)
}