package xyz.jordanplayz158.ptd.server.module.ptd2.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD2Pokemon(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Pokemon>(PTD2Pokemons)

    var save by PTD2Pokemons.save
    var swfId by PTD2Pokemons.swfId

    var number by PTD2Pokemons.number
    var nickname by PTD2Pokemons.nickname
    var experience by PTD2Pokemons.experience
    var level by PTD2Pokemons.level
    var move1 by PTD2Pokemons.move1
    var move2 by PTD2Pokemons.move2
    var move3 by PTD2Pokemons.move3
    var move4 by PTD2Pokemons.move4
    var targetType by PTD2Pokemons.targetType
    var tag by PTD2Pokemons.tag
    var position by PTD2Pokemons.position
    var gender by PTD2Pokemons.gender
    var extra by PTD2Pokemons.extra
    var item by PTD2Pokemons.item

    val createdAt by PTD2Pokemons.createdAt
    var updatedAt by PTD2Pokemons.updatedAt
}

object PTD2Pokemons : LongIdTableWithTimestamps("ptd2_pokemon") {
    val save = reference("save_id", PTD2Saves)

    val swfId = integer("swf_id")
    val number = short("number")
    val nickname = varchar("nickname", 25)
    val experience = integer("experience").default(0)
    val level = byte("level").default(1)
    val move1 = short("move_1")
    val move2 = short("move_2")
    val move3 = short("move_3")
    val move4 = short("move_4")
    val targetType = byte("target_type")
    val tag = varchar("tag", 2).default("h")
    val position = integer("position")
    val gender = byte("gender")
    val extra = byte("extra")
    val item = byte("item")

    init {
        index(true, save, swfId)
    }
}