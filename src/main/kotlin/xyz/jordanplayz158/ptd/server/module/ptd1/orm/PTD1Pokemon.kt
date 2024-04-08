package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD1Pokemon(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1Pokemon>(PTD1Pokemons)

    var save by PTD1Pokemons.save
    var swfId by PTD1Pokemons.swfId
    var number by PTD1Pokemons.number
    // May remove in future as SWF doesn't
    // seem to use it
    var nickname by PTD1Pokemons.nickname
    var experience by PTD1Pokemons.experience
    var level by PTD1Pokemons.level
    var move1 by PTD1Pokemons.move1
    var move2 by PTD1Pokemons.move2
    var move3 by PTD1Pokemons.move3
    var move4 by PTD1Pokemons.move4
    var moveSelected by PTD1Pokemons.moveSelected
    var ability by PTD1Pokemons.ability
    var targetType by PTD1Pokemons.targetType
    var tag by PTD1Pokemons.tag
    var position by PTD1Pokemons.position
    var rarity by PTD1Pokemons.rarity
    val createdAt by PTD1Pokemons.createdAt
    var updatedAt by PTD1Pokemons.updatedAt
}

object PTD1Pokemons : LongIdTableWithTimestamps("ptd1_pokemon") {
    val save = reference("save_id", PTD1Saves)
    // Remember this is auto_increment in code would
    // mark as such but from the sounds of it exposed
    // may try to make it auto_increment in the database
    val swfId = integer("swf_id")
    val number = short("number")
    val nickname = varchar("nickname", 30)
    val experience = integer("experience").default(0)
    val level = short("level").default(1)
    val move1 = short("move_1").default(1)
    val move2 = short("move_2").default(0)
    val move3 = short("move_3").default(0)
    val move4 = short("move_4").default(0)
    val moveSelected = short("move_selected").default(1)
    val ability = short("ability").default(0)
    val targetType = short("target_type").default(1)
    val tag = varchar("tag", 3).default("h")
    val position = integer("position").default(1)
    val rarity = short("rarity").default(0)

    init {
        index(true, save, swfId)
    }
}