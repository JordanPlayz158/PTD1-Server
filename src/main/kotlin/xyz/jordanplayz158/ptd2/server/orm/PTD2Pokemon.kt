package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.Pokes

class PTD2Pokemon(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Pokemon>(Pokes)

    var story by Pokes.story
    var swfId by Pokes.swfId

    var nickname by Pokes.nickname
    var num by Pokes.num
    var xp by Pokes.xp
    var lvl by Pokes.lvl
    var move1 by Pokes.move1
    var move2 by Pokes.move2
    var move3 by Pokes.move3
    var move4 by Pokes.move4
    var targetType by Pokes.targetType
    var gender by Pokes.gender
    var pos by Pokes.pos
    var extra by Pokes.extra
    var item by Pokes.item
    var tag by Pokes.tag
}