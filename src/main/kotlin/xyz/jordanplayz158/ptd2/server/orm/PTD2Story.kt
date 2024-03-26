package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.Extras
import xyz.jordanplayz158.ptd2.server.Items
import xyz.jordanplayz158.ptd2.server.Pokes
import xyz.jordanplayz158.ptd2.server.Stories

class PTD2Story(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Story>(Stories)

    var account by Stories.account

    var num by Stories.num
    var nickname by Stories.nickname
    var color by Stories.color
    var gender by Stories.gender
    var money by Stories.money
    var mapLoc by Stories.mapLoc
    var mapSpot by Stories.mapSpot
    var currentSave by Stories.currentSave
    var currentTime by Stories.currentTime

    val pokes by PTD2Pokemon referrersOn Pokes.story
    val items by PTD2Item referrersOn Items.story
    val extras by PTD2Extra referrersOn Extras.story
}