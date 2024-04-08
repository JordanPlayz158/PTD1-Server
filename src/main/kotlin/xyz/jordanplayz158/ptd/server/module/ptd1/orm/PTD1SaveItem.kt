package xyz.jordanplayz158.ptd.server.module.ptd1.orm

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import xyz.jordanplayz158.ptd.server.common.orm.LongIdTableWithTimestamps

class PTD1SaveItem(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PTD1SaveItem>(PTD1SaveItems)

    var save by PTD1SaveItems.save
    var item: PTD1ItemsEnum by PTD1SaveItems.item.transform({ it.id }, { PTD1ItemsEnum.get(it) })
    var quantity by PTD1SaveItems.quantity
    val createdAt by PTD1SaveItems.createdAt
    var updatedAt by PTD1SaveItems.updatedAt
}

object PTD1SaveItems : LongIdTableWithTimestamps("ptd1_save_items") {
    val save = reference("save_id", PTD1Saves)
    val item = short("item").uniqueIndex()
    val quantity = byte("quantity").default(1)

    init {
        index(true, save, item)
    }
}

enum class PTD1ItemsEnum(val id: Short) {
    // Stones
    MOON_STONE(1),
    LEAF_STONE(2),
    THUNDER_STONE(3),
    WATER_STONE(4),
    FIRE_STONE(5),

    // Fishing Rods
    OLD_ROD(6),
    SUPER_ROD(8),


    UNKNOWN1(25),
    // 1 is when all shiny pokemon on team
    UNKNOWN2(251),
    // 2 is when all shadow pokemon on team
    UNKNOWN3(252),

    UNKNOWN4(26),
    UNKNOWN5(261),
    UNKNOWN6(262),

    UNKNOWN7(27),
    UNKNOWN8(271),
    UNKNOWN9(272);

    companion object {
        fun get(id: Short): PTD1ItemsEnum {
            return PTD1ItemsEnum.entries.first { it.id == id }
        }
    }
}