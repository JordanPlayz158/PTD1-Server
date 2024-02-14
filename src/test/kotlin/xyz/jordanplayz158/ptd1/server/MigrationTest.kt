package xyz.jordanplayz158.ptd1.server

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Ulimit
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.jordanplayz158.ptd1.server.controller.SWFController
import xyz.jordanplayz158.ptd1.server.migration.SQLMigration
import xyz.jordanplayz158.ptd1.server.orm.Pokemon
import xyz.jordanplayz158.ptd1.server.orm.User
import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
class MigrationTest {
    companion object {
        private const val DATABASE = "test"
        private const val USERNAME = "test"
        private const val PASSWORD = "test"
    }


    @TempDir
    var tempDir: File? = null

    @Test
    fun testSqLiteMigration() {
        val dbFile = File(tempDir, DATABASE)
        println("SQLite: $dbFile")

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:$dbFile.db"
        val dataSource = HikariDataSource(config)

        val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
        SQLMigration(dataSource.connection, getCorrectFile("db/migration/$databaseServer", true))

        Database.connect(dataSource)
        accountTest()
    }

    @Test
    fun testMariaDbMigration() {
        val mariaDbContainer = MariaDBContainer("mariadb:11")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        mariaDbContainer.start()

        val config = HikariConfig()
        config.jdbcUrl = mariaDbContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
        SQLMigration(dataSource.connection, getCorrectFile("db/migration/$databaseServer", true))

        Database.connect(dataSource)
        accountTest()
    }

    @Test
    fun testMySqlMigration() {
        val mySqlContainer = MySQLContainer("mysql:8.0-debian")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            // Fix for some distros with wrong ulimits
            .withCreateContainerCmdModifier {cmd: CreateContainerCmd ->
                cmd.hostConfig?.withUlimits(arrayOf(
                    Ulimit("nofile", 20000L, 40000L),
                    Ulimit("nproc", 65535L, 65535L)
                ))
            }

        mySqlContainer.start()

        // Using Hikari because Flyway can't seem to
        //   figure out how to handle them otherwise
        val config = HikariConfig()
        config.jdbcUrl = mySqlContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
        SQLMigration(dataSource.connection, getCorrectFile("db/migration/$databaseServer", true))

        Database.connect(dataSource)
        accountTest()
    }

    @Test
    fun testPostgreSqlMigration() {
        val postgreSqlContainer = PostgreSQLContainer("postgres:16")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        postgreSqlContainer.start()

        val config = HikariConfig()
        config.jdbcUrl = postgreSqlContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
        SQLMigration(dataSource.connection, getCorrectFile("db/migration/$databaseServer", true))

        Database.connect(dataSource)
        accountTest()
    }

    private fun accountTest() {
        // Make account
        assertTrue(SWFController.createAccount("test", "test").first)
        // Try to make it again, should fail due to account existing check
        assertFalse(SWFController.createAccount("test", "test").first)

        transaction {
            val user = User.find(Users.email eq "test").with(User::saves).firstOrNull()
            assertNotNull(user)

            assertEquals(1, user.achievement.count())
            assertEquals(3, user.saves.count())

            val saves = user.saves

            // Update save entries and add some pokemon
            for(saveNumber in 0..2) {
                val save = saves.filter { save -> save.number.toInt() == saveNumber }[0]

                for(i in 1..5) {
                    Pokemon.new {
                        this.save = save.id
                        swfId = (save.pokemon.maxByOrNull { pokemon -> pokemon.swfId }?.swfId ?: 0) + 1
                        number = Random.nextInt(1, 152).toShort()
                        nickname = "${i}Pokemon"
                    }
                }
            }

            for (save in saves) {
                assertEquals(5, save.pokemon.count())
            }
        }
    }
}