package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.Achievements

class Achievement(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Achievement>(Achievements)

    var user by Achievements.user
    var shinyHunterRattata by Achievements.shinyHunterRattata
    var shinyHunterPidgey by Achievements.shinyHunterPidgey
    var shinyHunterGeodude by Achievements.shinyHunterGeodude
    var shinyHunterZubat by Achievements.shinyHunterZubat
    var starWars by Achievements.starWars
    var noAdvantage by Achievements.noAdvantage
    var rowinWithoutWindleId by Achievements.winWithoutWind
    var needsMoreCandy by Achievements.needsMoreCandy
    var theHardWay by Achievements.theHardWay
    var pewterChallenge by Achievements.pewterChallenge
    var ceruleanChallenge by Achievements.ceruleanChallenge
    var vermillionChallenge by Achievements.vermillionChallenge
    var celadonChallenge by Achievements.celadonChallenge
    var saffronCityChallenge by Achievements.saffronCityChallenge
    var fuchsiaGymChallenge by Achievements.fuchsiaGymChallenge
    var cinnabarGymChallenge by Achievements.cinnabarGymChallenge
    var viridianCityChallenge by Achievements.viridianCityChallenge
    val createdAt by Achievements.createdAt
    var updatedAt by Achievements.updatedAt
}