package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.Sessions

class Session(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Session>(Sessions)

    var sessionId by Sessions.sessionId
    var data by Sessions.data
    val createdAt by Sessions.createdAt
    var updatedAt by Sessions.updatedAt
}