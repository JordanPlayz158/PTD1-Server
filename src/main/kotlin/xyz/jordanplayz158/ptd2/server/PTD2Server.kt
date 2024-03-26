package xyz.jordanplayz158.ptd2.server

import at.favre.lib.crypto.bcrypt.BCrypt
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.ReasonsEnum
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd.getCorrectFile
import xyz.jordanplayz158.ptd2.server.controller.PTD2SWFController
import xyz.jordanplayz158.ptd2.server.orm.PTD2Account
import kotlin.concurrent.thread

val dotenv = dotenv {
     filename = ".env.ptd2"
}
//lateinit var dataSource: HikariDataSource

fun Application.ptd2() {
    //dataSource = HikariDataSource(databaseConfig(dotenv))

    //val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)

    //embeddedServer(CIO, port = dotenv["PORT", "8080"].toInt()) {
        //SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd2/$databaseServer", developmentMode))

        //val database = Database.connect(dataSource)

//        install(Sessions) {
//            cookie<UserSession>("ptd2_session", SQLSessionStorage())
//        }

        routing {
            staticFiles("/", getCorrectFile("static", developmentMode))

            route("/php") {
                post("/ptd2_save_12.php") {
                    val parameters = call.receiveParameters()

                    val action = parameters["Action"]
                    val email = parameters["Email"]
                    val password = parameters["Pass"]
                    //val ver = parameters["Ver"]
                    if (action == null || email == null || password == null/* || ver == null*/) {
                        call.respondText(
                            listOf(
                                "Result" to ResultsEnum.FAILURE.id,
                                "Reason" to "NotAllParametersSupplied"
                            ).formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )
                        return@post
                    }

                    if (action == "createAccount") {
                        call.respondText(
                            PTD2SWFController.createAccount(
                                email,
                                password
                            ).second.formUrlEncode(), ContentType.Application.FormUrlEncoded
                        )
                        return@post
                    }

                    val account = validAccount(email, password)
                    if (account === null) {
                        // Probably should be switched to a proper failure one if one exists
                        call.respondText(listOf("Result" to ResultsEnum.FAILURE.id, "Reason" to ReasonsEnum.FAILURE_NOT_FOUND.id).formUrlEncode(),
                            ContentType.Application.FormUrlEncoded)
                        return@post
                    }

                    // Non profile dependent
                    when (action) {
                        "loadAccount" -> call.respondText(PTD2SWFController.loadAccount().second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                        "loadStory" -> call.respondText(PTD2SWFController.loadStory(account).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                        "load1on1" -> call.respondText(PTD2SWFController.load1v1(account).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)

                        else -> {
                            val whichProfileString = parameters["whichProfile"]
                            if (whichProfileString == null) {
                                call.respondText(
                                    listOf(
                                        "Result" to ResultsEnum.FAILURE.id,
                                        "Reason" to "NotAllParametersSupplied"
                                    ).formUrlEncode(), ContentType.Application.FormUrlEncoded
                                )
                                return@post
                            }

                            val whichProfile = whichProfileString.toInt()
                            when (action) {
                                "loadStoryProfile" -> call.respondText(PTD2SWFController.loadStoryProfile(account, whichProfile).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                                "deleteStory" -> call.respondText(PTD2SWFController.deleteStory(account, whichProfile).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                                "saveStory" -> call.respondText(PTD2SWFController.saveStory(account, whichProfile, parameters).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                                "save1on1" -> call.respondText(PTD2SWFController.save1v1(account, whichProfile, parameters).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                                "delete1on1" -> call.respondText(PTD2SWFController.delete1v1(account, whichProfile).second.formUrlEncode(), ContentType.Application.FormUrlEncoded)
                            }
                        }
                    }
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            println("Shutdown signal received. Exiting...")
            //TransactionManager.closeAndUnregister(database)
            //dataSource.close()
        })
    //}.start(wait = true)
}

// Probably replace with ID
fun getAvailableSaveId(email: String): Long {
    return transaction { PTD2Account.find(Accounts.email eq email).first().stories.first().pokes.maxByOrNull { poke -> poke.id }?.id?.value ?: (0 + 1) }
}

/**
 * @return true if user exists AND user's password matches
 */
fun validCredentials(email: String, password: String) : Boolean {
    val user = transaction { PTD2Account.find(Accounts.email eq email).firstOrNull() }

    if(user === null) return false

    return BCrypt.verifyer().verify(password.toCharArray(), user.pass).verified
}

/**
 * @return PTD2Account object if account exists and correct password is supplied, otherwise null
 */
fun validAccount(email: String, password: String) : PTD2Account? {
    // Not the most effort as validCredentials throws away the account effectively
    // But it may be cached so could be minimal but I would probably have validAccount manually
    //  check and then return so no double call
    if (validCredentials(email, password)) {
        return transaction { PTD2Account.find(Accounts.email eq email).firstOrNull() }
    }

    return null
}