package xyz.jordanplayz158.ptd.server.module.ptd2.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD2Save(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Save>(PTD2Saves)

    var user by PTD2Saves.user

    var number by PTD2Saves.number
    var currentMap by PTD2Saves.currentMap
    var mapSpot by PTD2Saves.mapSpot
    var nickname by PTD2Saves.nickname
    var gender by PTD2Saves.gender
    var money by PTD2Saves.money
    //var currentSave by PTD2Saves.currentSave
    var currentTime by PTD2Saves.currentTime
    var version by PTD2Saves.version

    val pokemon by PTD2Pokemon referrersOn PTD2Pokemons.save
    val extras by PTD2Extra referrersOn PTD2Extras.save

    fun items(): List<PTD2Extra> {
        return transaction { extras.filter { it.isItem } }
    }

    fun extras(): List<PTD2Extra> {
        return transaction { extras.filterNot { it.isItem } }
    }
}

object PTD2Saves : LongIdTableWithTimestamps("ptd2_saves") {
    val user = reference("user_id", PTD2Users)

    val number = byte("number")
    val currentMap = byte("current_map").default(3)
    val mapSpot = byte("map_spot").default(1)
    val nickname = varchar("nickname", 40)
    val gender = byte("gender")
    val money = integer("money").default(10)
    //val currentSave = varchar("current_save", 15).default(0)
    val currentTime = byte("current_time")
    val version = byte("version")

    init {
        index(true, user, number)
    }
}