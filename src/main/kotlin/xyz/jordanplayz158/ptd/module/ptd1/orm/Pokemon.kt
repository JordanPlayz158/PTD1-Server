package xyz.jordanplayz158.ptd.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.module.ptd1.Pokemons

class Pokemon(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Pokemon>(Pokemons)

    var save by Pokemons.save
    var swfId by Pokemons.swfId
    var number by Pokemons.number
    // May remove in future as SWF doesn't
    // seem to use it
    var nickname by Pokemons.nickname
    var experience by Pokemons.experience
    var level by Pokemons.level
    var move1 by Pokemons.move1
    var move2 by Pokemons.move2
    var move3 by Pokemons.move3
    var move4 by Pokemons.move4
    var moveSelected by Pokemons.moveSelected
    var ability by Pokemons.ability
    var targetType by Pokemons.targetType
    var tag by Pokemons.tag
    var position by Pokemons.position
    var rarity by Pokemons.rarity
    val createdAt by Pokemons.createdAt
    var updatedAt by Pokemons.updatedAt
}