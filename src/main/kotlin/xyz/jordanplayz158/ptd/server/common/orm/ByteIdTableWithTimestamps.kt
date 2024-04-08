package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

open class ByteIdTableWithTimestamps(table: String) : IdTable<Byte>(table) {
    override val id: Column<EntityID<Byte>> = byte("id").autoIncrement().entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}