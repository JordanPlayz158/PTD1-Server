package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD1AchievementRedemption(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1AchievementRedemption>(PTD1AchievementRedemptions)

    val achievement by PTD1AchievementRedemptions.achievement
    val completions by PTD1AchievementRedemptions.completions
    var redemptions by PTD1AchievementRedemptions.redemptions
    val createdAt by PTD1AchievementRedemptions.createdAt
    var updatedAt by PTD1AchievementRedemptions.updatedAt
}

object PTD1AchievementRedemptions : LongIdTableWithTimestamps("ptd1_achievement_redemptions") {
    val achievement = reference("achievement_id", PTD1Achievements)
    val completions = short("completions").default(0)
    val redemptions = short("redemptions").default(0)
}