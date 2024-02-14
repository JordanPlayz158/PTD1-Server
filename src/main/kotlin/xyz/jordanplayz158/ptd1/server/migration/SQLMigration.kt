package xyz.jordanplayz158.ptd1.server.migration

import java.io.File
import java.sql.Connection
import java.sql.SQLException

// As Flyway doesn't support GraalVM and (it seems) Liquibase doesn't support GraalVM
// I have decided to make my own, simple solution which should work under GraalVM
// especially as I don't need many of the features offered, only a way to migrate using .sql files
class SQLMigration(private val connection: Connection, migrationDirectory: File) {
    init {
        createMigrationsTable()

        for (file in migrationDirectory.walkTopDown()) {
            val fileName = file.name

            if (file.isDirectory || migrationExists(fileName)) continue

            if (!migrateFile(file)) {
                println("Migration '${fileName}' failed, not proceeding")
                break
            }

            println("Migration '${fileName}' succeeded")
        }

        connection.close()
    }

    private fun createMigrationsTable() {
        connection.createStatement().use {
            try {
                it.execute("SELECT 1 FROM migrations LIMIT 1")
                // Assume the table was found, if so, no need to make the migrations table
                return
            } catch (_: SQLException) {
                it.execute("""CREATE TABLE migrations (
                    file VARCHAR(255)
                );""")
            }
        }
    }

    private fun migrationExists(file: String) : Boolean {
        connection.createStatement().use {
            val result = it.executeQuery("SELECT * FROM migrations WHERE file = '$file'")

            return result.next()
        }
    }

    private fun migrateFile(file: File) : Boolean {
        connection.createStatement().use { statement ->
            var statements = file.readText().split(";")

            // The split may result in a trailing empty string causing SQLException
            // So we check for it, and remove it if it exists
            if (statements.last().isBlank()) {
                statements = statements.dropLast(1)
            }

            statements.forEach(statement::addBatch)

            for (executeBatch in statement.executeBatch()) {
                if (executeBatch < 0) return false
            }

            val rowsModified = statement.executeUpdate("INSERT INTO migrations VALUES ('${file.name}')")

            if (rowsModified == 1) {
                return true
            }

            return false
        }
    }
}