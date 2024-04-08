package xyz.jordanplayz158.ptd.server.module.ptd1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.*
import xyz.jordanplayz158.ptd.server.module.ptd1.controller.PTD1SWFController
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1Save
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1User
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1Users

fun Application.ptd1() {
    routing {
        route("/php") {
            post("/newPoke8.php") {
                val parameters = call.receiveParameters()

                val action = parameters["Action"]
                val email = parameters["Email"]
                val password = parameters["Pass"]
                val ver = parameters["Ver"]
                if (action == null || email == null || password == null || ver == null) {
                    call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_ALL_PARAMETERS_SUPPLIED.fullReason())
                    return@post
                }

                if (action == "createAccount") {
                    call.respondUrlEncodedForm(PTD1SWFController.createAccount(email, password))
                    return@post
                }

                if(!validCredentials(email, password)) {
                    call.respondUrlEncodedForm(ReasonsEnum.FAILURE_NOT_FOUND)
                    return@post
                }

                val ptdUser = getOrCreateUser(email, password)
                val user = transaction { PTD1User.find(PTD1Users.user eq ptdUser.id).with(PTD1User::saves, PTD1Save::pokemon, PTD1Save::items).first() }

                when (action) {
                    "loadAccount" -> call.respondUrlEncodedForm(PTD1SWFController.loadAccount(user))

                    "saveAccount" -> call.respondUrlEncodedForm(PTD1SWFController.saveAccount(user, parameters))
                }
            }
        }
    }
}

/**
 * @return true if user exists AND user's password matches
 */
fun validCredentials(email: String, password: String) : Boolean {
    val user = getUser(email)

    if (user === null) return false

    val ptd1User = transaction { PTD1User.find(PTD1Users.user eq user.id).firstOrNull() }

    if(ptd1User === null) return false

    return passwordVerify(password, user.password)
}