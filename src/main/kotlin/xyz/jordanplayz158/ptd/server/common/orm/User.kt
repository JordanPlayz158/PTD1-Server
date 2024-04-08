package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)

    var email by Users.email
    var role by Users.role
    var password by Users.password
    val createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
}

object Users : LongIdTableWithTimestamps("users") {
    val email = varchar("email", 50).uniqueIndex()
    val role = reference("role_id", Roles)
    val password = varchar("password", 72)
}