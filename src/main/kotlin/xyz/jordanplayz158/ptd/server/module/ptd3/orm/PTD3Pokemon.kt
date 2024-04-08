package xyz.jordanplayz158.ptd.server.module.ptd3.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD3Pokemon(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD3Pokemon>(PTD3Pokemons)

    var save by PTD3Pokemons.save

    var swfId by PTD3Pokemons.swfId
    var number by PTD3Pokemons.number
    var nickname by PTD3Pokemons.nickname
    var experience by PTD3Pokemons.experience
    var level by PTD3Pokemons.level
    var move1 by PTD3Pokemons.move1
    var move2 by PTD3Pokemons.move2
    var move3 by PTD3Pokemons.move3
    var move4 by PTD3Pokemons.move4
    var targetType by PTD3Pokemons.targetType
    var gender by PTD3Pokemons.gender
    var position by PTD3Pokemons.position
    var extra by PTD3Pokemons.extra
    var item by PTD3Pokemons.item
    var tag by PTD3Pokemons.tag
    var moveSelected by PTD3Pokemons.moveSelected
    var ability by PTD3Pokemons.ability
    val createdAt by PTD3Pokemons.createdAt
    var updatedAt by PTD3Pokemons.updatedAt
}

object PTD3Pokemons : LongIdTableWithTimestamps("ptd3_pokemons") {
    val save = reference("save_id", PTD3Saves)

    val swfId = integer("swf_id")
    val number = short("number")
    val nickname = varchar("nickname", 40)
    val experience = integer("experience").default(0)
    val level = short("level").default(1)
    val move1 = short("move_1")
    val move2 = short("move_2")
    val move3 = short("move_3")
    val move4 = short("move_4")
    val moveSelected = byte("move_selected").default(1)
    val targetType = byte("target_type")
    val tag = varchar("tag", 2)
    val position = integer("position")
    val gender = byte("gender")
    val extra = byte("extra")
    val item = byte("item")
    val ability = short("ability").default(0)
}