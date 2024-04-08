package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps
import java.time.Instant

class PTD1Achievement(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1Achievement>(PTD1Achievements)

    var user by PTD1Achievements.user
    var shinyHunterRattata by PTD1Achievements.shinyHunterRattata
    var shinyHunterPidgey by PTD1Achievements.shinyHunterPidgey
    var shinyHunterGeodude by PTD1Achievements.shinyHunterGeodude
    var shinyHunterZubat by PTD1Achievements.shinyHunterZubat
    var starWars by PTD1Achievements.starWars
    var noAdvantage by PTD1Achievements.noAdvantage
    var rowinWithoutWindleId by PTD1Achievements.winWithoutWind
    var needsMoreCandy by PTD1Achievements.needsMoreCandy
    var theHardWay by PTD1Achievements.theHardWay
    var pewterChallenge by PTD1Achievements.pewterChallenge
    var ceruleanChallenge by PTD1Achievements.ceruleanChallenge
    var vermillionChallenge by PTD1Achievements.vermillionChallenge
    var celadonChallenge by PTD1Achievements.celadonChallenge
    var saffronCityChallenge by PTD1Achievements.saffronCityChallenge
    var fuchsiaGymChallenge by PTD1Achievements.fuchsiaGymChallenge
    var cinnabarGymChallenge by PTD1Achievements.cinnabarGymChallenge
    var viridianCityChallenge by PTD1Achievements.viridianCityChallenge
    val createdAt by PTD1Achievements.createdAt
    var updatedAt by PTD1Achievements.updatedAt
}

object PTD1Achievements : LongIdTableWithTimestamps("ptd1_achievements") {
    val user = reference("user_id", PTD1Users)
    val shinyHunterRattata = bool("shiny_hunter_rattata").default(false)
    val shinyHunterPidgey = bool("shiny_hunter_pidgey").default(false)
    val shinyHunterGeodude = bool("shiny_hunter_geodude").default(false)
    val shinyHunterZubat = bool("shiny_hunter_zubat").default(false)
    val starWars = bool("star_wars").default(false)
    val noAdvantage = bool("no_advantage").default(false)
    val winWithoutWind = bool("win_without_wind").default(false)
    val needsMoreCandy = bool("needs_more_candy").default(false)
    val theHardWay = bool("the_hard_way").default(false)
    val pewterChallenge = bool("pewter_challenge").default(false)
    val ceruleanChallenge = bool("cerulean_challenge").default(false)
    val vermillionChallenge = bool("vermillion_challenge").default(false)
    val celadonChallenge = bool("celadon_challenge").default(false)
    val saffronCityChallenge = bool("saffron_city_challenge").default(false)
    val fuchsiaGymChallenge = bool("fuchsia_gym_challenge").default(false)
    val cinnabarGymChallenge = bool("cinnabar_gym_challenge").default(false)
    val viridianCityChallenge = bool("viridian_city_challenge").default(false)
}