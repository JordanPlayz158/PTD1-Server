package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Setting(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Setting>(Settings)

    var key by Settings.key
    var value by Settings.value
    val createdAt by Settings.createdAt
    var updatedAt by Settings.updatedAt
}

object Settings : LongIdTableWithTimestamps("settings") {
    val key = varchar("key", 255).uniqueIndex()
    val value = varchar("value", 255)
}