package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.Achievements
import xyz.jordanplayz158.ptd1.server.Saves
import xyz.jordanplayz158.ptd1.server.Users

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)

    var email by Users.email
    var role by Users.role
    var password by Users.password
    var dex by Users.dex
    var shinyDex by Users.shinyDex
    var shadowDex by Users.shadowDex
    val createdAt by Users.createdAt
    var updatedAt by Users.updatedAt

    val saves by Save referrersOn Saves.user
    val achievement by Achievement referrersOn Achievements.user
}