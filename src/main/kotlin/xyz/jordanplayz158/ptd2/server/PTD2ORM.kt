package xyz.jordanplayz158.ptd2.server

import org.jetbrains.exposed.dao.id.LongIdTable

object Accounts : LongIdTable("accounts") {
    val email = varchar("email", 50).uniqueIndex()
    val pass = varchar("pass", 255)
    val dex1 = varchar("dex1", 151).nullable()
    val dex2 = varchar("dex2", 100).nullable()
    val dex3 = varchar("dex3", 135).nullable()
    val dex4 = varchar("dex4", 107).nullable()
    val dex5 = varchar("dex5", 156).nullable()
    val dex6 = varchar("dex6", 90).nullable()
}

object Stories : LongIdTable("stories") {
    // A little misleading, a story will always have a reference to
    //  an account but for the optional reference FROM account, it needs
    //  to be nullable
    val account = reference("account_id", Accounts).nullable()

    val num = byte("num")
    val nickname = varchar("Nickname", 40)
    val color = byte("Color")
    val gender = byte("Gender")
    val money = integer("Money")
    val mapLoc = short("MapLoc")
    val mapSpot = short("MapSpot")
    val currentSave = varchar("CurrentSave", 15)
    val currentTime = short("CurrentTime")
}

object Pokes : LongIdTable("pokes") {
    val story = reference("story_id", Stories)

    val swfId = integer("swf_id")
    val nickname = varchar("Nickname", 25)
    val num = short("num")
    val xp = integer("xp")
    val lvl = short("lvl")
    val move1 = short("move1")
    val move2 = short("move2")
    val move3 = short("move3")
    val move4 = short("move4")
    val targetType = byte("targetingType")
    val gender = byte("gender")
    val pos = integer("pos")
    val extra = byte("extra")
    val item = byte("item")
    val tag = varchar("tag", 2)

    init {
        index(true, story, swfId)
    }
}

object Extras : LongIdTable("extras") {
    val story = reference("story_id", Stories)

    val num = byte("num")
    val value = integer("value")
}

object Items : LongIdTable("items") {
    val story = reference("story_id", Stories)

    val num = byte("num")
    val value = integer("value")
}

object OneV1S : LongIdTable("1v1s") {
    val account = reference("account_id", Accounts).nullable()

    var num = byte("num")
    var money = integer("money")
    var levelUnlocked = byte("levelUnlocked")
}