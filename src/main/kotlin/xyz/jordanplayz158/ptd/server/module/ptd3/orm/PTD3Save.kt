package xyz.jordanplayz158.ptd.server.module.ptd3.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps
import xyz.jordanplayz158.ptd.server.common.orm.Users

class PTD3Save(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD3Save>(PTD3Saves)

    var user by PTD3Saves.user

    var number by PTD3Saves.num
    var levelsCompleted by PTD3Saves.levelsCompleted
    var levelsAccomplished by PTD3Saves.levelsAccomplished
    var nickname by PTD3Saves.nickname
    var money by PTD3Saves.money
    var gender by PTD3Saves.gender
    var version by PTD3Saves.version
    val createdAt by PTD3Saves.createdAt
    var updatedAt by PTD3Saves.updatedAt

    val pokemons by PTD3Pokemon referrersOn PTD3Pokemons.save
    val extras by PTD3Extra referrersOn PTD3Extras.save

    fun items(): List<PTD3Extra> {
        return extras.filter { it.isItem }
    }

    fun extras(): List<PTD3Extra> {
        return extras.filterNot { it.isItem }
    }
}

object PTD3Saves : LongIdTableWithTimestamps("ptd3_saves") {
    val user = reference("user_id", Users)

    val num = byte("number")
    val levelsCompleted = byte("levels_completed").default(1)
    // Could mean levels started but not completed?
    val levelsAccomplished = byte("levels_accomplished").default(1)
    val nickname = varchar("nickname", 40)
    val money = integer("money").default(10)
    val gender = byte("gender")
    val version = byte("version")
}