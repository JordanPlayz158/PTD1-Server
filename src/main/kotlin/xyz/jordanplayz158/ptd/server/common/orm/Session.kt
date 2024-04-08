package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Session(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Session>(Sessions)

    var sessionId by Sessions.sessionId
    var data by Sessions.data
    val createdAt by Sessions.createdAt
    var updatedAt by Sessions.updatedAt
}

object Sessions : LongIdTableWithTimestamps("sessions") {
    val sessionId = varchar("session_id", 255).uniqueIndex()
    val data = text("data").nullable()
}
