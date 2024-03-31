package xyz.jordanplayz158.ptd.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.module.ptd1.Achievements
import xyz.jordanplayz158.ptd.module.ptd1.Saves
import xyz.jordanplayz158.ptd.module.ptd1.Users

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