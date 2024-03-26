package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.Extras

class PTD2Extra(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Extra>(Extras)

    var story by Extras.story

    var num by Extras.num
    var value by Extras.value
}