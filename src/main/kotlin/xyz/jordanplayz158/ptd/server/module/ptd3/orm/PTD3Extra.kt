package xyz.jordanplayz158.ptd.server.module.ptd3.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps
import xyz.jordanplayz158.ptd.server.module.ptd2.orm.PTD2OneV1S

class PTD3Extra(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD3Extra>(PTD3Extras)

    var save by PTD3Extras.save

    var isItem by PTD3Extras.isItem
    var number by PTD3Extras.num
    var value by PTD3Extras.value

    val createdAt by PTD3Extras.createdAt
    var updatedAt by PTD2OneV1S.updatedAt
}

object PTD3Extras : LongIdTableWithTimestamps("ptd3_extras") {
    val save = reference("save_id", PTD3Saves)

    val isItem = bool("is_item")
    val num = short("number")
    val value = integer("value")
}