package xyz.jordanplayz158.ptd.server

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Ulimit
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.jordanplayz158.ptd.server.common.orm.User
import xyz.jordanplayz158.ptd.server.common.orm.Users
import xyz.jordanplayz158.ptd.server.migration.SQLMigration
import xyz.jordanplayz158.ptd.server.module.ptd1.controller.PTD1SWFController
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1Pokemon
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1User
import xyz.jordanplayz158.ptd.server.module.ptd1.orm.PTD1Users
import xyz.jordanplayz158.ptd.server.module.ptd2.controller.PTD2SWFController
import xyz.jordanplayz158.ptd.server.module.ptd3.controller.PTD3SWFController
import java.io.File
import java.sql.Connection
import java.util.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        val dbFile = File(tempDir, "$DATABASE.db")
        println("SQLite: $dbFile")

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:$dbFile"
        val dataSource = HikariDataSource(config)

        migration(dataSource.connection, 0)
        sqlite(dataSource, 1)
        sqlite(dataSource, 2)
        sqlite(dataSource, 3)
    }

    private fun sqlite(dataSource: HikariDataSource, ptd: Int) {
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

        migration(dataSource(mariaDbContainer.jdbcUrl).connection, 0)
        testContainers(mariaDbContainer.jdbcUrl, 1)
        testContainers(mariaDbContainer.jdbcUrl, 2)
        testContainers(mariaDbContainer.jdbcUrl, 3)
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

        migration(dataSource(mySqlContainer.jdbcUrl).connection, 0)
        testContainers(mySqlContainer.jdbcUrl, 1)
        testContainers(mySqlContainer.jdbcUrl, 2)
        testContainers(mySqlContainer.jdbcUrl, 3)
    }

    @Test
    fun testPostgreSqlMigration() {
        val postgreSqlContainer = PostgreSQLContainer("postgres:16")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        postgreSqlContainer.start()

        migration(dataSource(postgreSqlContainer.jdbcUrl).connection, 0)
        testContainers(postgreSqlContainer.jdbcUrl, 1)
        testContainers(postgreSqlContainer.jdbcUrl, 2)
        testContainers(postgreSqlContainer.jdbcUrl, 3)
    }

    private fun testContainers(url: String, ptd: Int) {
        val dataSource = dataSource(url)
        migration(dataSource.connection, ptd)

        Database.connect(dataSource)
        accountTest(ptd)
    }

    fun dataSource(url: String): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = url
        config.username = USERNAME
        config.password = PASSWORD

        return HikariDataSource(config)
    }

    private fun migration(connection: Connection, ptd: Int) {
        val databaseServer = connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)

        val path = getCorrectFile("db/migration/ptd" + when (ptd) {
            0 -> {
                "/$databaseServer"
            }
            else -> {
                "$ptd/$databaseServer"
            }
        }, true)


        SQLMigration(connection, path)
    }

    private fun accountTest(ptd: Int) {
         when (ptd) {
            1 -> {
                // Make account
                val successResult = PTD1SWFController.createAccount("test", "test")
                    .formUrlEncode().parseUrlEncodedParameters()

                assertEquals("Success", successResult["Result"])

                // Try to make it again, should fail due to account existing check
                val failureResult = PTD1SWFController.createAccount("test", "test")
                    .formUrlEncode().parseUrlEncodedParameters()

                assertEquals("Failure", failureResult["Result"])


                transaction {
                    val user = User.find(Users.email eq "test").first()
                    val ptd1User = PTD1User.find(PTD1Users.user eq user.id).with(PTD1User::saves).firstOrNull()
                    assertNotNull(ptd1User)

                    assertEquals(1, ptd1User.achievement.count())
                    assertEquals(3, ptd1User.saves.count())

                    val saves = ptd1User.saves

                    // Update save entries and add some pokemon
                    for(saveNumber in 0..2) {
                        val save = saves.filter { save -> save.number.toInt() == saveNumber }[0]

                        for(i in 1..5) {
                            PTD1Pokemon.new {
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
                val successResult = PTD2SWFController.createAccount("test", "test")
                    .formUrlEncode().parseUrlEncodedParameters()

                assertEquals("Success", successResult["Result"])

                val failureResult = PTD2SWFController.createAccount("test", "test")
                    .formUrlEncode().parseUrlEncodedParameters()

                assertEquals("Failure", failureResult["Result"])
            }

            3 -> {
                // PTD3 uses the common user entity so this should fail as PTD1 made PTD common user entry
                val failureResult = PTD3SWFController.createAccount("test", "test")
                    .formUrlEncode().parseUrlEncodedParameters()

                assertEquals("Failure", failureResult["Result"])
            }
            else -> throw RuntimeException("Invalid ptd value '$ptd'")
        }

    }
}