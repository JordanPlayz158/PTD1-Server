package xyz.jordanplayz158.ptd.server

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
import xyz.jordanplayz158.ptd.getCorrectFile
import xyz.jordanplayz158.ptd.migration.SQLMigration
import xyz.jordanplayz158.ptd.module.ptd1.Users
import xyz.jordanplayz158.ptd.module.ptd1.controller.SWFController
import xyz.jordanplayz158.ptd.module.ptd1.orm.Pokemon
import xyz.jordanplayz158.ptd.module.ptd1.orm.User
import xyz.jordanplayz158.ptd2.server.controller.PTD2SWFController
import java.io.File
import java.sql.Connection
import java.util.*
import kotlin.random.Random
import kotlin.test.*

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
        // Can't figure out why it is broken but all others seem to work?
        //  so could be a bug in something sqlite specific
        sqlite(1)
        sqlite(2)
    }

    private fun sqlite(ptd: Int) {
        val dbFile = File(tempDir, "$DATABASE$ptd.db")
        println("SQLite: $dbFile")

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:$dbFile"
        val dataSource = HikariDataSource(config)

        migration(dataSource.connection, ptd)

        Database.connect(dataSource)

//        TransactionManager.manager.defaultIsolationLevel =
//            Connection.TRANSACTION_SERIALIZABLE

        accountTest(ptd)
    }

    @Test
    fun testMariaDbMigration() {
        val mariaDbContainer = MariaDBContainer("mariadb:11")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        mariaDbContainer.start()

        testContainers(mariaDbContainer.jdbcUrl, 1)
        //testContainers(mariaDbContainer.jdbcUrl, 2)
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

        testContainers(mySqlContainer.jdbcUrl, 1)
        //testContainers(mariaDbContainer.jdbcUrl, 2)
    }

    @Test
    fun testPostgreSqlMigration() {
        val postgreSqlContainer = PostgreSQLContainer("postgres:16")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        postgreSqlContainer.start()

        testContainers(postgreSqlContainer.jdbcUrl, 1)
        //testContainers(mariaDbContainer.jdbcUrl, 2)
    }

    private fun testContainers(url: String, ptd: Int) {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = USERNAME
        config.password = PASSWORD

        val dataSource = HikariDataSource(config)
        migration(dataSource.connection, ptd)

        Database.connect(dataSource)
        accountTest(ptd)
    }

    private fun migration(connection: Connection, ptd: Int) {
        val databaseServer = connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
        SQLMigration(connection, getCorrectFile("db/migration/ptd$ptd/$databaseServer", true))
    }

    private fun accountTest(ptd: Int) {
         when (ptd) {
            1 -> {
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
            2 -> {
                // Make account
                assertTrue(PTD2SWFController.createAccount("test", "test").first)
                // Try to make it again, should fail due to account existing check
                assertFalse(PTD2SWFController.createAccount("test", "test").first)
            }
            else -> throw RuntimeException("Invalid ptd value '$ptd'")
        }

    }
}