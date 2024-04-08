package xyz.jordanplayz158.ptd.server.module.ptd2

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.ReasonsEnum
import xyz.jordanplayz158.ptd.server.common.getUser
import xyz.jordanplayz158.ptd.server.common.passwordVerify
import xyz.jordanplayz158.ptd.server.common.respondUrlEncodedForm
import xyz.jordanplayz158.ptd.server.module.ptd2.controller.PTD2SWFController
import xyz.jordanplayz158.ptd.server.module.ptd2.orm.PTD2User
import xyz.jordanplayz158.ptd.server.module.ptd2.orm.PTD2Users

fun Application.ptd2() {
    routing {
        route("/php") {
            post("/ptd2_save_12.php") {
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
                    call.respondUrlEncodedForm(PTD2SWFController.createAccount(email, password))
                    return@post
                }

                val account = validAccount(email, password)
                if (account === null) {
                    call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_FOUND)
                    return@post
                }

                // Non profile dependent
                when (action) {
                    "loadAccount" -> call.respondUrlEncodedForm(PTD2SWFController.loadAccount())
                    "loadStory" -> call.respondUrlEncodedForm(PTD2SWFController.loadStory(account))
                    "load1on1" -> call.respondUrlEncodedForm(PTD2SWFController.load1v1(account))

                    else -> {
                        val whichProfileString = parameters["whichProfile"]
                        if (whichProfileString == null) {
                            call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED)
                            return@post
                        }

                        val whichProfile = whichProfileString.toByte()

                        when (action) {
                            "saveStory" -> call.respondUrlEncodedForm(
                                PTD2SWFController.saveStory(account, whichProfile, parameters))

                            "save1on1" -> call.respondUrlEncodedForm(
                                PTD2SWFController.save1v1(account, whichProfile, parameters))

                            "delete1on1" -> call.respondUrlEncodedForm(
                                PTD2SWFController.delete1v1(transaction { account.oneV1.firstOrNull { it.number == whichProfile } }))
                        }

                        val story = transaction { account.saves.firstOrNull { it.number == whichProfile } }

                        if (story === null) {
                            call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_FOUND)
                            return@post
                        }

                        when (action) {
                            "loadStoryProfile" -> call.respondUrlEncodedForm(
                                PTD2SWFController.loadStoryProfile(story))

                            "deleteStory" -> call.respondUrlEncodedForm(
                                PTD2SWFController.deleteStory(story))
                        }
                    }
                }
            }
        }
    }
}

/**
 * @return PTD2Account object if account exists and correct password is supplied, otherwise null
 */
fun validAccount(email: String, password: String) : PTD2User? {
    val user = getUser(email)

    if (user === null) return null

    val ptd2User = transaction { PTD2User.find(PTD2Users.user eq user.id).firstOrNull() }

    if(ptd2User === null) return null

    if(passwordVerify(password, user.password)) {
        return ptd2User
    }

    return null
}