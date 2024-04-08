package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.SaveVersionEnum
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD1Save(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1Save>(PTD1Saves)

    var user by PTD1Saves.user
    var number by PTD1Saves.number
    var levelsCompleted by PTD1Saves.levelsCompleted
    var levelsStarted by PTD1Saves.levelsStarted
    var nickname by PTD1Saves.nickname
    var badges by PTD1Saves.badges
    var avatar by PTD1Saves.avatar
    var hasFlash by PTD1Saves.hasFlash
    var challenge by PTD1Saves.challenge
    var money by PTD1Saves.money
    var npcTrade by PTD1Saves.npcTrade
    var version: SaveVersionEnum by PTD1Saves.version.transform({ it.id }, { SaveVersionEnum.get(it) })
    val createdAt by PTD1Saves.createdAt
    var updatedAt by PTD1Saves.updatedAt

    val items by PTD1SaveItem referrersOn PTD1SaveItems.save
    val pokemon by PTD1Pokemon referrersOn PTD1Pokemons.save
}

object PTD1Saves : LongIdTableWithTimestamps("ptd1_saves") {
    val user = reference("user_id", PTD1Users)
    val number = byte("number")
    val levelsCompleted = byte("levels_completed").default(0)
    val levelsStarted = byte("levels_started").default(0)
    val nickname = varchar("nickname", 8).default("Satoshi")
    val badges = byte("badges").default(0)
    val avatar = varchar("avatar", 4).default("none")
    val hasFlash = bool("has_flash").default(false)
    val challenge = byte("challenge").default(0)
    val money = integer("money").default(50)
    val npcTrade = bool("npc_trade").default(false)
    val version = byte("version")

    init {
        index(true, user, number)
    }
}