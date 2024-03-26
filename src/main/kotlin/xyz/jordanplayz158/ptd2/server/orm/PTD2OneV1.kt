package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.OneV1S

class PTD2OneV1(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2OneV1>(OneV1S)

    var account by OneV1S.account

    var num by OneV1S.num
    var money by OneV1S.money
    var levelUnlocked by OneV1S.levelUnlocked
}