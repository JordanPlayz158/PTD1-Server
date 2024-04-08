package xyz.jordanplayz158.ptd.server.common.orm

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

open class LongIdTableWithTimestamps(table: String) : LongIdTable(table) {
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()
}