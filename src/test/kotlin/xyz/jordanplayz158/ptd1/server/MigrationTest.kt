package xyz.jordanplayz158.ptd1.server

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Ulimit
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
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
        private var TESTCONTAINERS = true
        private var DATABASE = "test"
        private var USERNAME = "test"
        private var PASSWORD = "test"

        init {
            val usesTestContainers = System.getenv("USES_TESTCONTAINERS")
            if(usesTestContainers !== null) TESTCONTAINERS = usesTestContainers.toBoolean()

            val db = System.getenv("DATABASE_DB")
            if(db !== null) DATABASE = db

            val username = System.getenv("DATABASE_USER")
            if(username !== null) USERNAME = username

            val password = System.getenv("DATABASE_PASS")
            if(password !== null) PASSWORD = password
        }
    }


    @TempDir
    var tempDir: File? = null

    @Test
    fun testSqLiteMigration() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${File(tempDir, DATABASE)}.db"
        val dataSource = HikariDataSource(config)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/" + dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH))
            .load()
            .migrate()

        Database.connect(dataSource)
        accountTest()
    }

    @Test
    fun testMariaDbMigration() {
        val mariaDbContainer = MariaDBContainer("mariadb:11")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        if(TESTCONTAINERS) mariaDbContainer.start()

        val config = HikariConfig()
        config.jdbcUrl = mariaDbContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/" + dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH))
            .load()
            .migrate()

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

        if(TESTCONTAINERS) mySqlContainer.start()

        // Using Hikari because Flyway can't seem to
        //   figure out how to handle them otherwise
        val config = HikariConfig()
        config.jdbcUrl = mySqlContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/" + dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH))
            .load()
            .migrate()

        Database.connect(dataSource)
        accountTest()
    }

    @Test
    fun testPostgreSqlMigration() {
        val postgreSqlContainer = PostgreSQLContainer("postgres:16")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)

        if(TESTCONTAINERS) postgreSqlContainer.start()

        val config = HikariConfig()
        config.jdbcUrl = postgreSqlContainer.getJdbcUrl()
        config.username = USERNAME
        config.password = PASSWORD
        val dataSource = HikariDataSource(config)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/" + dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH))
            .load()
            .migrate()

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