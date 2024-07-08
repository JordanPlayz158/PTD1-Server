package xyz.jordanplayz158.ptd.server.module.ptd3

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
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.jordanplayz158.ptd.server.getCorrectFile
import xyz.jordanplayz158.ptd.server.migration.SQLMigration
import java.io.File
import java.util.*
import kotlin.test.assertEquals

@Testcontainers
class EtrottaTest {
    companion object {
        private lateinit var pythonUrl: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            val pythonContainer = GenericContainer(
                ImageFromDockerfile().withDockerfile(
                    File("src/test/resources/xyz/jordanplayz158/ptd/server/module/ptd3/Dockerfile").toPath()))
                .withExposedPorts(8080)
                .withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("EtrottaTest")))

            pythonContainer.start()

            pythonUrl = "http://${pythonContainer.host}:${pythonContainer.firstMappedPort}"

            val dbFile = File.createTempFile(System.currentTimeMillis().toString(), "ptd3-test.db")
            // Doesn't work?
            dbFile.deleteOnExit()

            val config = HikariConfig()
            config.jdbcUrl = "jdbc:sqlite:$dbFile"
            val dataSource = HikariDataSource(config)

            val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
            SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd/$databaseServer", true))
            SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd3/$databaseServer", true))

            Database.connect(dataSource)

            // Ensure 1 account is created for tests, reduces redundancy
            createAccount()
        }

        fun createAccount(email: String = "test", password: String = "test") {
            val body = "Pass=$password&Email=$email&Action=createAccount&ver=31"
            val old = runOld(body = body).parseUrlEncodedParameters()
            val new = runNew(body = body).parseUrlEncodedParameters()

            // Also can't do it as etrotta hardcoded success responses
            //  and reasons (where applicable)
            // PTD3 has a random UID thing so can't just compare contents
            //assertEquals(old["Result"], new["Result"])
            //assertEquals(old["Reason"], new["Reason"])
        }

        private fun runNew(file: String = "/php/ptd3_save_1.php", body: String): String {
            var new: String? = null
            testApplication {
                application {
                    ptd3()
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
        fun runOld(path: String = "/php/ptd3_save_1.php", body: String): String {
            return runBlocking {
                return@runBlocking client.post("$pythonUrl$path") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }.bodyAsText()
            }
        }
    }

    @Test
    fun createAccountSuccess() {
        // Sadly this test doesn't really have any benefit
        //  because the responses are hardcoded
        createAccount("testCreateAccountSuccess")
    }

    @Test
    fun createAccountTaken() {
        // Sadly this test doesn't really have any benefit
        //  because the responses are hardcoded
        createAccount()
    }

//    @Test
//    fun loadAccountNotFound() {
//        // Sadly this test doesn't really have any benefit
//        //  because the responses are hardcoded
//        loadAccount("notFound")
//    }

    @Test
    fun loadAccountPopulated() {
        val (old, new) = loadAccount()
        val oldParameters = old.parseUrlEncodedParameters()
        val newParameters = new.parseUrlEncodedParameters()

        assertEquals(oldParameters["Result"], newParameters["Result"])
        assertEquals(oldParameters["Reason"], newParameters["Reason"])
    }

    fun loadAccount(email: String = "test"): Pair<String, String> {
        val body = "Pass=test&Email=$email&Action=loadAccount&ver=31"
        val old = runOld(body = body)
        val new = runNew(body = body)
        return old to new
    }

//    @Test
//    fun loadStoryEmpty() {
//        // Etrotta's response has extra data than necessary
//        //  so the assertEquals won't work for this
//        loadStory()
//    }

    fun loadStory(email: String = "test") {
        val body = "Pass=test&Email=$email&Action=loadStory&ver=31"
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

    @Test
    fun loadStoryProfilePopulated() {
        createAccount("testLoadStoryProfilePopulated")
        saveStory("testLoadStoryProfilePopulated", 1)
        loadStoryProfile("testLoadStoryProfilePopulated", 1)

    }

    fun loadStoryProfile(email: String, whichProfile: Int) {
        val body = "Action=loadStoryProfile&Email=$email&Pass=test&ver=152&whichProfile=$whichProfile"
        val old = runOld(body = body)
        val new = runNew(body = body)

        val oldParameters = old.parseUrlEncodedParameters()
        val newParameters = new.parseUrlEncodedParameters()

        assertEquals(oldParameters["Result"], newParameters["Result"])
        assertEquals(oldParameters["extra"], newParameters["extra"])
        assertEquals(oldParameters["extra2"], newParameters["extra2"])
        assertEquals(oldParameters["PN1"], newParameters["PN1"])
        assertEquals(oldParameters["extra4"], newParameters["extra4"])

    }

    @Test
    fun saveStoryFresh() {
        createAccount("testSaveStory")
        saveStory("testSaveStory", 1)
    }

    @Test
    fun saveStoryOverwriteExisting() {
        createAccount("testSaveStory")

        saveStory("testSaveStory", 1)
        saveStory("testSaveStory", 1)
    }

    fun saveStory(email: String, whichProfile: Int) {
        val body = "extra2=yqym" +
                "&extra3=wawyyyyyymyycyrwyymyawwwcqapycymyyyyymymymynyyymymym" +
                "&ver=31" +
                "&Email=$email" +
                "&Action=saveStory" +
                "&whichProfile=$whichProfile" +
                "&extra5=cwqc" +
                "&extra4=yqym" +
                "&Pass=test" +
                "&extra=Save%3Dtrue%26NewGameSave%3Dtrue%26Nickname%3DShigeru%26Color%3D1%26Gender%3D0%26CS%3Dnull%26MSave%3Dtrue%26MA%3Dym%26PokeNick1%3DPichu"

        val old = runOld(body = body)
        val new = runNew(body = body)

        val oldParameters = old.parseUrlEncodedParameters()
        val newParameters = new.parseUrlEncodedParameters()

        assertEquals(oldParameters["Result"], newParameters["Result"])
        assertEquals(oldParameters["CS"], newParameters["CS"])
    }

//    @Test
//    fun deleteStory() {
//        // Sadly this test doesn't really have any benefit
//        //  because the responses are hardcoded
//
//    }
}