package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps
import xyz.jordanplayz158.ptd.server.common.orm.Users

class PTD1User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1User>(PTD1Users)

    var user by PTD1Users.user

    var dex by PTD1Users.dex
    var shinyDex by PTD1Users.shinyDex
    var shadowDex by PTD1Users.shadowDex
    val createdAt by PTD1Users.createdAt
    var updatedAt by PTD1Users.updatedAt

    val saves by PTD1Save referrersOn PTD1Saves.user
    val achievement by PTD1Achievement referrersOn PTD1Achievements.user
}

object PTD1Users : LongIdTableWithTimestamps("ptd1_users") {
    val user = reference("user_id", Users)

    val dex = varchar("dex", 151).default(fillDex())
    val shinyDex = varchar("shiny_dex", 151).default(fillDex())
    val shadowDex = varchar("shadow_dex", 151).default(fillDex())
}

fun fillDex(dex: String = "", length: Int = 151): String {
    val dex = StringBuilder(dex)

    while (dex.length < length) {
        dex.append(0)
    }

    return dex.toString()
}