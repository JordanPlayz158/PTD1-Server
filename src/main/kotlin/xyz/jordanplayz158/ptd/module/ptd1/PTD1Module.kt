package xyz.jordanplayz158.ptd.module.ptd1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.jordanplayz158.ptd.ResultsEnum
import xyz.jordanplayz158.ptd.module.ptd1.controller.SWFController

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
}