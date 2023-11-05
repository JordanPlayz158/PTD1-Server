package xyz.jordanplayz158.ptd1.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd1.server.AchievementRedemptions

class AchievementRedemption(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AchievementRedemption>(AchievementRedemptions)

    val achievement by AchievementRedemptions.achievement
    val completions by AchievementRedemptions.completions
    var redemptions by AchievementRedemptions.redemptions
    val createdAt by AchievementRedemptions.createdAt
    var updatedAt by AchievementRedemptions.updatedAt
}