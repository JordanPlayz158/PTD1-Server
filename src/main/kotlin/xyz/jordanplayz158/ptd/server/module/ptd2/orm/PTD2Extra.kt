package xyz.jordanplayz158.ptd.server.module.ptd2.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD2Extra(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Extra>(PTD2Extras)

    var save by PTD2Extras.save

    var isItem by PTD2Extras.isItem
    var number by PTD2Extras.number
    var value by PTD2Extras.value

    val createdAt by PTD2Extras.createdAt
    var updatedAt by PTD2Extras.updatedAt
}

object PTD2Extras : LongIdTableWithTimestamps("ptd2_extras") {
    val save = reference("save_id", PTD2Saves)

    val isItem = bool("is_item")
    val number = short("number")
    val value = integer("value")
}