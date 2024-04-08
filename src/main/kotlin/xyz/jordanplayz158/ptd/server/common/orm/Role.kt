package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Role(id: EntityID<Byte>) : Entity<Byte>(id) {
    companion object : EntityClass<Byte, Role>(Roles)

    // Tried to do Roles.id.transform, no dice but this should be safe as name has unique constraint
    var enum: RolesEnum by Roles.name.transform({ it.name }, { RolesEnum.valueOf(it) })
    var name by Roles.name
    val createdAt by Roles.createdAt
    var updatedAt by Roles.updatedAt
}

object Roles : ByteIdTableWithTimestamps("roles") {
    val name = varchar("name", 255).uniqueIndex()
}

enum class RolesEnum(val id: Byte) {
    ADMIN(1),
    USER(100);

    companion object {
        fun get(id: Byte): RolesEnum {
            return entries.first { it.id == id }
        }
    }
}