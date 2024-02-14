package xyz.jordanplayz158.ptd1.server.csrf

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.sessions.CurrentSession
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import xyz.jordanplayz158.ptd1.server.session.UserSession
import java.security.SecureRandom
import kotlin.random.Random

enum class CSRFTokenType {
    PER_SESSION,
    PER_REQUEST
}

//class CSRFProtection {
//    class CSRFConfig {
//        val tokenType = CSRFTokenType.PER_REQUEST
//        val parameterName = "_csrf"
//    }
//
//    companion object {
//        val config = CSRFConfig()
//    }
//}

class CSRFConfig {
    val tokenType = CSRFTokenType.PER_REQUEST
    val parameterName = "_csrf"
}

val CSRFProtection = createApplicationPlugin(
    name = "CSRFProtection",
    createConfiguration = ::CSRFConfig
) {
    val parameterName = pluginConfig.parameterName
    val tokenType = pluginConfig.tokenType

    on(CallSetup) {call ->
        val session = call.sessions

        when (tokenType) {
            CSRFTokenType.PER_SESSION -> {
                if (session.get<UserSession>() == null) {
                    session.set(UserSession(CSRFToken(parameterName)))
                }
            }

            CSRFTokenType.PER_REQUEST -> {
                session.set(UserSession(CSRFToken(parameterName)))
            }
        }
    }
}

class CSRFToken(parameterName: String, token: String = secureRandomStringByKotlinRandom(128))

fun csrfMapOf(session: CurrentSession, vararg pairs: Pair<String, Any>): Map<String, Any> {
//    val config = CSRFProtection.config
//
//    when (config.tokenType) {
//        CSRFTokenType.PER_SESSION -> {
//            if (session.get("_csrf") == null) {
//                session.set("_csrf", CSRFToken(config.parameterName, secureRandomStringByKotlinRandom(128)))
//            }
//        }
//        CSRFTokenType.PER_REQUEST -> {
//            session.set("_csrf", CSRFToken(config.parameterName, secureRandomStringByKotlinRandom(128)))
//        }
//    }


    return mapOf("_csrf" to session.get<UserSession>()?.csrf!!, *pairs)
}

val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
fun randomStringByKotlinRandom(length: Int) = (1..length)
    .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
    .joinToString("")

fun secureRandomStringByKotlinRandom(length: Int) : String {
    val secureRandom = SecureRandom()

    return (1..length)
        .map { secureRandom.nextInt(0, charPool.size).let { charPool[it] } }
        .joinToString("")
}