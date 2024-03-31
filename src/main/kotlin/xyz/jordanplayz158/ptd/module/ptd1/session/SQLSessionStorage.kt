package xyz.jordanplayz158.ptd.module.ptd1.session

import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.module.ptd1.Sessions
import xyz.jordanplayz158.ptd.module.ptd1.orm.Session

class SQLSessionStorage : SessionStorage {
    override suspend fun invalidate(id: String) {
        transaction {
            Session.find{ Sessions.sessionId eq id }.firstOrNull()?.delete()
        }
    }

    override suspend fun read(id: String): String {
        var data: String? = null

        transaction {
            data = Session.find{ Sessions.sessionId eq id }.firstOrNull()?.data ?: throw NoSuchElementException("Session $id not found")
        }

        return data!!
    }

    override suspend fun write(id: String, value: String) {
        transaction {
            Session.find { Sessions.sessionId eq id }.firstOrNull()?.data = value
        }
    }
}