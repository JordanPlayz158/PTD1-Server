package xyz.jordanplayz158.ptd.module.ptd1

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Roles : LongIdTable("roles") {
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now()).nullable()
}

object Users : LongIdTable("users") {
    val email = varchar("email", 50).uniqueIndex()
    val role = reference("role_id", Roles)
    val password = varchar("password", 72)
    val dex = varchar("dex", 151).default("")
    val shinyDex = varchar("shiny_dex", 151).default("")
    val shadowDex = varchar("shadow_dex", 151).default("")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object Saves : LongIdTable("saves") {
    val user = reference("user_id", Users)
    val number = short("number")
    val levelsCompleted = short("levels_completed").default(0)
    val levelsStarted = short("levels_started").default(0)
    val nickname = varchar("nickname", 8).default("Satoshi")
    val badges = short("badges").default(0)
    val avatar = varchar("avatar", 4).default("none")
    val hasFlash = bool("has_flash").default(false)
    val challenge = short("challenge").default(0)
    val money = integer("money").default(50)
    val npcTrade = bool("npc_trade").default(false)
    val version = short("version")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object SaveItems : LongIdTable("save_items") {
    val save = reference("save_id", Saves)
    val item = short("item").uniqueIndex()
    val count = short("count").default(1)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

// I am pure evil
object Pokemons : LongIdTable("pokemon") {
    val save = reference("save_id", Saves)
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
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()

    init {
        index(true, save, swfId)
    }
}

object Achievements : LongIdTable("achievements") {
    val user = reference("user_id", Users)
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
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object AchievementRedemptions : LongIdTable("achievement_redemptions") {
    val achievement = reference("achievement_id", Achievements)
    val completions = short("completions").default(0)
    val redemptions = short("redemptions").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object Sessions : LongIdTable("sessions") {
    val sessionId = varchar("sessionId", 255).uniqueIndex()
    val data = text("data").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}

object Settings : LongIdTable("settings") {
    val key = varchar("key", 255).uniqueIndex()
    val value = varchar("value", 255)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}