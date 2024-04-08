package xyz.jordanplayz158.ptd.server.module.ptd2.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps
import xyz.jordanplayz158.ptd.server.common.orm.Users

class PTD2User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2User>(PTD2Users)

    var user by PTD2Users.user

    var dex1 by PTD2Users.dex1
    var dex2 by PTD2Users.dex2
    var dex3 by PTD2Users.dex3
    var dex4 by PTD2Users.dex4
    var dex5 by PTD2Users.dex5
    var dex6 by PTD2Users.dex6

    val saves by PTD2Save referrersOn PTD2Saves.user
    val oneV1 by PTD2OneV1 referrersOn PTD2OneV1S.user
}

object PTD2Users : LongIdTableWithTimestamps("ptd2_users") {
    val user = reference("user_id", Users)

    val dex1 = varchar("gen_1_dex", 151).nullable().default("")
    val dex2 = varchar("gen_2_dex", 100).nullable().default("")
    val dex3 = varchar("gen_3_dex", 135).nullable().default("")
    val dex4 = varchar("gen_4_dex", 107).nullable().default("")
    val dex5 = varchar("gen_5_dex", 156).nullable().default("")
    val dex6 = varchar("gen_6_dex", 90).nullable().default("")
}