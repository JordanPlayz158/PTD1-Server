package xyz.jordanplayz158.ptd2.server.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd2.server.Accounts
import xyz.jordanplayz158.ptd2.server.OneV1S
import xyz.jordanplayz158.ptd2.server.Stories

class PTD2Account(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD2Account>(Accounts)

    var email by Accounts.email
    var pass by Accounts.pass
    var dex1 by Accounts.dex1
    var dex2 by Accounts.dex2
    var dex3 by Accounts.dex3
    var dex4 by Accounts.dex4
    var dex5 by Accounts.dex5
    var dex6 by Accounts.dex6

    // TODO: Find out a way to get exposed (or kotlin) to identify the field as nullable
    val stories by PTD2Story optionalReferrersOn Stories.account
    val oneV1 by PTD2OneV1 optionalReferrersOn OneV1S.account
}