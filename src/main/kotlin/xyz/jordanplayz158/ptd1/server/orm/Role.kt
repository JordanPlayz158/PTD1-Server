package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.Roles

class Role(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Role>(Roles)

    var name by Roles.name
    val createdAt by Roles.createdAt
    var updatedAt by Roles.updatedAt


}