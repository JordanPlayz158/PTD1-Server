package xyz.jordanplayz158.ptd2.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.jordanplayz158.ptd.getCorrectFile
import xyz.jordanplayz158.ptd.migration.SQLMigration
import java.io.File
import java.util.*
import kotlin.test.assertEquals

@Testcontainers
class KGMTest {
    companion object {
        private lateinit var phpUrl: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            val network = Network.newNetwork()

            val phpDb = MariaDBContainer("mariadb:11")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
                .withNetwork(network)
                .withNetworkAliases("phpdb")
            phpDb.start()

            val phpContainer = GenericContainer("php:8.3.4-apache")
                // Yes is unsafe but only for testing
                .withFileSystemBind(File("php/").absolutePath, "/var/www/html", BindMode.READ_ONLY)
                //.withEnv("DB_HOST", "${phpDb.host}:${phpDb.firstMappedPort}")
                .withNetwork(network)
                //.withEnv("DB_HOST", "phpdb:${phpDb.firstMappedPort}")
                .withEnv("DB_HOST", "phpdb")
                .withEnv("DB_USER", phpDb.username)
                .withEnv("DB_PASS", phpDb.password)
                .withEnv("DB_NAME", phpDb.databaseName)
                .dependsOn(phpDb)
                .withExposedPorts(80)

            phpContainer.start()
            phpContainer.execInContainer("docker-php-ext-install", "mysqli")
            phpContainer.execInContainer("apachectl", "restart")

            phpUrl = "http://${phpContainer.host}:${phpContainer.firstMappedPort}"

            val dbFile = File.createTempFile(System.currentTimeMillis().toString(), "ptd2-test.db")
            // Doesn't work?
            //dbFile.deleteOnExit()

            val config = HikariConfig()
            config.jdbcUrl = "jdbc:sqlite:$dbFile"
            val dataSource = HikariDataSource(config)

            val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
            SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd2/$databaseServer", true))

            Database.connect(dataSource)

            // Ensure 1 account is created for tests, reduces redundancy
            createAccount()
        }

        fun createAccount(email: String = "test", password: String = "test") {
            val body = "Action=createAccount&Email=$email&Pass=$password&ver=152"
            val old = runOld(body = body)
            val new = runNew(body = body)

            assertEquals(old, new)
        }

        private fun runNew(file: String = "/php/ptd2_save_12.php", body: String): String {
            var new: String? = null
            testApplication {
                application {
                    ptd2()
                }
                val response = client.post(file) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }.bodyAsText()

                new = response
            }

            return new!!
        }

        private val client = HttpClient()
        fun runOld(path: String = "/public/php/ptd2_save_12.php", body: String): String {
            return runBlocking {
                return@runBlocking client.post("$phpUrl$path") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }.bodyAsText()
            }
        }

//        @JvmStatic
//        @AfterAll
//        fun cleanUp() {
//            Thread.sleep(Long.MAX_VALUE)
//        }
    }

    @Test
    fun createAccountSuccess() {
        createAccount("testCreateAccountSuccess")
    }

    @Test
    fun createAccountTaken() {
        createAccount()
    }

    @Test
    fun loadAccountNotFound() {
        val body = "Action=loadAccount&Email=notFound&Pass=notFound&ver=152"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadAccount() {
        val body = "Action=loadAccount&Email=test&Pass=test&ver=152"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }


    /*
    // Some endpoints KGM doesn't check if password is correct
    //  Only for non-destructive actions though like loading
    //  but this is one place where my logic will diverge from KGM's
    @Test
    fun loadStoryWrongPassword() {
        createAccount("testLoadStoryWrongPassword", "wrongpassword")
        loadStory("testLoadStoryWrongPassword")
    }*/

    @Test
    fun loadStoryEmpty() {
        loadStory("test")
    }

    fun loadStory(email: String) {
        val body = "Action=loadStory&Email=$email&Pass=test&ver=152"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadStoryPartialPopulation() {
        createAccount("testLoadStoryPartialPopulation")

        saveStory("testLoadStoryPartialPopulation", 2)
        loadStory("testLoadStoryPartialPopulation")
    }

    @Test
    fun loadStoryFullyPopulated() {
        createAccount("testLoadStoryFullyPopulated")

        for (i in 1..3) {
            saveStory("testLoadStoryFullyPopulated", i)
        }

        loadStory("testLoadStoryFullyPopulated")
    }

    // KGM's has an error so testing against it.... doesn't really make sense
//    @Test
//    fun loadStoryProfileEmpty() {
//        loadStoryProfile("test", 1)
//    }

    @Test
    fun loadStoryProfilePopulated() {
        createAccount("testLoadStoryProfilePopulated")
        saveStory("testLoadStoryProfilePopulated", 1)
        loadStoryProfile("testLoadStoryProfilePopulated", 1)
    }

    @Test
    fun loadStoryProfileWithPokemon() {
        createAccount("testLoadStoryProfileWithPokemon")
        // saveStory with pokemon does not specify it is a new story but existing
        //  works as existing story save test and pokemon save
        saveStory("testLoadStoryProfileWithPokemon", 1)
        saveStoryWithPokemon("testLoadStoryProfileWithPokemon", 1)
        loadStoryProfile("testLoadStoryProfileWithPokemon", 1)
    }

    fun loadStoryProfile(email: String, whichProfile: Int) {
        val body = "Action=loadStoryProfile&Email=$email&Pass=test&ver=152&whichProfile=$whichProfile"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun saveStoryFresh() {
        // Destructive things should have independent profiles
        // Even though so long as both do the same things, should be fine
        createAccount("testSaveStory")
        saveStory("testSaveStory", 1)
    }

    @Test
    fun saveStoryOverwriteExisting() {
        // Destructive things should have independent profiles
        // Even though so long as both do the same things, should be fine
        createAccount("testSaveStory")

        saveStory("testSaveStory", 1)
        saveStory("testSaveStory", 1)
    }

    fun saveStory(email: String, whichProfile: Int) {
        val body = "extra=Save%3Dtrue%26NewGameSave%3Dtrue%26Nickname%3DGold%26Color%3D1%26Gender%3D1%26MapSave%3Dtrue%26MapLoc%3D1%26MapSpot%3D1%26CS%3Dnull" +
                "&dextra5=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&Email=$email" +
                "&ver=152" +
                "&dextra3=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&dcextra5=pcmp" +
                "&dcextra1=pyyy" +
                "&Action=saveStory" +
                "&dextra6=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&extra5=qpw" +
                "&extra2=yqym" +
                "&dextra4=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&dcextra6=woaw" +
                "&extra3=yqym" +
                "&dcextra3=aqer" +
                "&dcextra4=qcoa" +
                "&dcextra2=qyww" +
                "&dextra2=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&needD=1" +
                "&whichProfile=$whichProfile" +
                "&dextra1=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&extra4=yqym" +
                "&Pass=test"

        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    fun saveStoryWithPokemon(email: String, whichProfile: Int) {
        val body = "extra=Save%3Dtrue%26MapSave%3Dtrue%26MapLoc%3D9%26MapSpot%3D1%26CS%3D0%26TimeSave%3Dtrue%26CT%3D105%26MSave%3Dtrue%26MA%3Dyy%26PokeNick1%3DCyndaquil%26PokeNick2%3DRattata" +
                "&dextra5=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&Email=$email" +
                "&ver=152" +
                "&dextra3=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&dcextra5=pywo" +
                "&dcextra1=aorm" +
                "&Action=saveStory" +
                "&dextra6=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&extra5=aycp" +
                "&extra2=wwayayyywywyyycyyyqyyyayy" +
                "&dextra4=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&dcextra6=wrra" +
                "&extra3=weaywyyyymyycyaaycqymwymyywqcwyrwymyyyyymymymynyyyymyywyoycywqyryqycyywywyqyyyyymymyn" +
                "&dcextra3=acym" +
                "&dcextra4=qwye" +
                "&dcextra2=coey" +
                "&dextra2=mmmymmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&needD=1" +
                "&whichProfile=$whichProfile" +
                "&dextra1=mmmmmmmmmmmmmmmmmmymmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                "&extra4=yeyyywyy" +
                "&Pass=test"

        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadOneOnOneEmpty() {
        loadOneOnOne("test")
    }

    fun loadOneOnOne(email: String) {
        val body = "Action=load1on1&Email=$email&Pass=test&ver=152"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadOneOnOnePartialPopulation() {
        createAccount("testLoadOneOnOnePartialPopulation")

        saveOneOnOne("testLoadOneOnOnePartialPopulation", 1)

        loadOneOnOne("testLoadOneOnOnePartialPopulation")
    }

    @Test
    fun loadOneOnOneFullyPopulated() {
        createAccount("testLoadOneOnOneFullyPopulated")

        saveOneOnOne("testLoadOneOnOneFullyPopulated", 1)
        saveOneOnOne("testLoadOneOnOneFullyPopulated", 2)
        saveOneOnOne("testLoadOneOnOneFullyPopulated", 3)

        loadOneOnOne("testLoadOneOnOneFullyPopulated")
    }

    @Test
    fun saveOneOnOneEmpty() {
        createAccount("testSaveOneOnOne")
        saveOneOnOne("testSaveOneOnOne", 1)
    }

    @Test
    fun saveOnOnOneOverwriteExisting() {
        createAccount("testSaveOneOnOneOverwriteExisting")
        saveOneOnOne("testSaveOneOnOneOverwriteExisting", 1)
        saveOneOnOne("testSaveOneOnOneOverwriteExisting", 1)
    }

    fun saveOneOnOne(email: String, whichProfile: Int) {
        val body = "extra=yrwwmyy" +
                "&Email=$email" +
                "&ver=152" +
                "&Action=save1on1" +
                "&whichProfile=$whichProfile" +
                "&extra2=yqym" +
                "&Pass=test"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun deleteOneOnOneEmpty() {
        createAccount("testDeleteOneOnOne")
        deleteOneOnOne("testDeleteOneOnOne", 1)
    }

    fun deleteOneOnOne(email: String, whichProfile: Int) {
        val body = "Email=$email" +
                "&Action=delete1on1" +
                "&ver=152" +
                "&whichProfile=$whichProfile" +
                "&Pass=test"
        val old = runOld(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun deleteOneOnOneExisting() {
        createAccount("testDeleteOneOnOneExisting")
        saveOneOnOne("testDeleteOneOnOneExisting", 1)
        deleteOneOnOne("testDeleteOneOnOneExisting", 1)
    }
}