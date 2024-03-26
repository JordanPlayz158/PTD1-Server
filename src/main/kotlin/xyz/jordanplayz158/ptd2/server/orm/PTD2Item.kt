package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.Items

class PTD2Item(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Item>(Items)

    var story by Items.story

    var num by Items.num
    var value by Items.value
}