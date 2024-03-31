package xyz.jordanplayz158.ptd.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.module.ptd1.SaveItems

class SaveItem(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SaveItem>(SaveItems)

    var save by SaveItems.save
    var item by SaveItems.item
    var count by SaveItems.count
    val createdAt by SaveItems.createdAt
    var updatedAt by SaveItems.updatedAt
}