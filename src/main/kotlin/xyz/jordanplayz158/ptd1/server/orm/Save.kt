package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.Pokemons
import xyz.jordanplayz158.ptd1.server.SaveItems
import xyz.jordanplayz158.ptd1.server.Saves

class Save(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Save>(Saves)

    var user by Saves.user
    var number by Saves.number
    var levelsCompleted by Saves.levelsCompleted
    var levelsStarted by Saves.levelsStarted
    var nickname by Saves.nickname
    var badges by Saves.badges
    var avatar by Saves.avatar
    var hasFlash by Saves.hasFlash
    var challenge by Saves.challenge
    var money by Saves.money
    var npcTrade by Saves.npcTrade
    var version by Saves.version
    val createdAt by Saves.createdAt
    var updatedAt by Saves.updatedAt

    val items by SaveItem referrersOn SaveItems.save
    val pokemon by Pokemon referrersOn Pokemons.save
}