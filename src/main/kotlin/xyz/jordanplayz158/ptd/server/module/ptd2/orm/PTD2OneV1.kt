package xyz.jordanplayz158.ptd.server.module.ptd2.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD2OneV1(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2OneV1>(PTD2OneV1S)

    var user by PTD2OneV1S.user

    var number by PTD2OneV1S.number
    var money by PTD2OneV1S.money
    var levelsUnlocked by PTD2OneV1S.levelsUnlocked

    val createdAt by PTD2OneV1S.createdAt
    var updatedAt by PTD2OneV1S.updatedAt
}

object PTD2OneV1S : LongIdTableWithTimestamps("ptd2_1v1s") {
    val user = reference("user_id", PTD2Users)

    var number = byte("number")
    var money = integer("money")
    var levelsUnlocked = byte("levels_unlocked")

    init {
        index(true, user, number)
    }
}