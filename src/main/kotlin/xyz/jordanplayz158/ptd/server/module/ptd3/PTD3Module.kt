package xyz.jordanplayz158.ptd.server.module.ptd3

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.ReasonsEnum
import xyz.jordanplayz158.ptd.server.common.orm.User
import xyz.jordanplayz158.ptd.server.common.orm.Users
import xyz.jordanplayz158.ptd.server.common.passwordVerify
import xyz.jordanplayz158.ptd.server.common.respondUrlEncodedForm
import xyz.jordanplayz158.ptd.server.module.ptd3.controller.PTD3SWFController

fun Application.ptd3() {
    routing {
        route("/php") {
            post("/ptd3_save_1.php") {
                val parameters = call.receiveParameters()

                val action = parameters["Action"]
                val email = parameters["Email"]
                val password = parameters["Pass"]
                //val ver = parameters["Ver"]
                if (action == null || email == null || password == null/* || ver == null*/) {
                    call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED)
                    return@post
                }

                if (action == "createAccount") {
                    call.respondUrlEncodedForm(PTD3SWFController.createAccount(email, password))
                    return@post
                }

                val account = validAccount(email, password)
                if (account === null) {
                    // Probably should be switched to a proper failure one if one exists
                    call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_FOUND)
                    return@post
                }

                // Non profile dependent
                when (action) {
                    "loadAccount" -> call.respondUrlEncodedForm(PTD3SWFController.loadAccount())
                    "loadStory" -> call.respondUrlEncodedForm(PTD3SWFController.loadStory(account))

                    else -> {
                        val whichProfileString = parameters["whichProfile"]
                        if (whichProfileString == null) {
                            call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED)
                            return@post
                        }

                        val whichProfile = whichProfileString.toByte()
                        when (action) {
                            "loadStoryProfile" -> call.respondUrlEncodedForm(
                                PTD3SWFController.loadStoryProfile(account, whichProfile))

                            "deleteStory" -> call.respondUrlEncodedForm(
                                PTD3SWFController.deleteStory(account, whichProfile))

                            "saveStory" -> call.respondUrlEncodedForm(
                                PTD3SWFController.saveStory(account, whichProfile, parameters))
                        }
                    }
                }
            }
        }

        route("/ptd3") {
            route("/gameFiles") {
                get("/get_mystery_gift.php") {
                    call.respondUrlEncodedForm(listOf("mgn" to "0", "mgs" to "0"))
                }
            }
        }
    }
}

/**
 * @return PTD3Account object if account exists and correct password is supplied, otherwise null
 */
fun validAccount(email: String, password: String) : User? {
    val user = transaction { User.find(Users.email eq email).firstOrNull() }

    if(user === null) return null

    if(passwordVerify(password, user.password)) {
        return user
    }

    return null
}