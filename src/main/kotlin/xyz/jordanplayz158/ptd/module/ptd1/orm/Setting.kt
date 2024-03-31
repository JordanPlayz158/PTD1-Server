package xyz.jordanplayz158.ptd.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.module.ptd1.Settings

class Setting(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Setting>(Settings)

    var key by Settings.key
    var value by Settings.value
    val createdAt by Settings.createdAt
    var updatedAt by Settings.updatedAt
}