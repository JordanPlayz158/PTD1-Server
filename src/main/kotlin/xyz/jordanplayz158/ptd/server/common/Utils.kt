package xyz.jordanplayz158.ptd.server.common

import at.favre.lib.crypto.bcrypt.BCrypt
import com.zaxxer.hikari.HikariConfig
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.orm.User
import xyz.jordanplayz158.ptd.server.common.orm.Users
import java.io.File

fun passwordHash(password: String): String {
    return BCrypt.withDefaults().hashToString(12, password.toCharArray())
}

fun passwordVerify(password: String, hashedPassword: String): Boolean {
    return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified
}

fun randomNum(range: LongRange): Long {
    return range.random()
}

/*
 * No password check
 */
fun getUser(email: String): User? {
    return transaction { User.find(Users.email eq email).firstOrNull() }
}

fun getUser(email: String, password: String): User? {
    val user = getUser(email)

    if (user === null || !passwordVerify(password, user.password)) {
        return null
    }

    return user
}

fun getOrCreateUser(email: String, password: String): User {
    return transaction {
        val user = getUser(email)

        if (user === null) {
            return@transaction User.new {
                this.email = email
                this.password = passwordHash(password)
            }
        }

        return@transaction user
    }
}

suspend fun ApplicationCall.respondUrlEncodedForm(response: List<Pair<String, String>>) {
    respondText(
        response.formUrlEncode(),
        ContentType.Application.FormUrlEncoded
    )
}

suspend fun ApplicationCall.respondUrlEncodedForm(response: ReasonsEnum) {
    respondUrlEncodedForm(response.fullReason())
}