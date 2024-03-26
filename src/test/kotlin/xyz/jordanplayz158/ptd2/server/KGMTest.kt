package xyz.jordanplayz158.ptd2.server

import com.caucho.quercus.QuercusContext
import com.caucho.quercus.env.Env
import com.caucho.quercus.lib.db.JavaSqlDriverWrapper
import com.caucho.quercus.lib.db.QuercusDataSource
import com.caucho.quercus.script.QuercusScriptEngine
import com.caucho.quercus.script.QuercusScriptEngineFactory
import com.caucho.quercus.servlet.api.QuercusHttpServletRequest
import com.caucho.util.NullEnumeration
import com.caucho.vfs.FilePath
import com.caucho.vfs.StringWriter
import com.caucho.vfs.WriteStream
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Testcontainers
import xyz.jordanplayz158.ptd.getCorrectFile
import xyz.jordanplayz158.ptd.migration.SQLMigration
import java.io.File
import java.util.*
import kotlin.test.assertEquals

@Testcontainers
class KGMTest {
    companion object {
        private val directory = File("php/public/php")

        lateinit var quercus: QuercusContext
        lateinit var request: QuercusHttpServletRequest
        lateinit var database: Database
        lateinit var dbFile: File

        @JvmStatic
        @BeforeAll
        fun setup() {
            val phpDb = MariaDBContainer("mariadb:11")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")
            phpDb.start()

            val factory = QuercusScriptEngineFactory()
            val engine = factory.scriptEngine as QuercusScriptEngine
            quercus = engine.quercus
            //quercus.setServerEnv("REQUEST_METHOD", "POST")

            val phpConfig = JavaSqlDriverWrapper(org.mariadb.jdbc.Driver(), phpDb.jdbcUrl)
            quercus.database = QuercusDataSource(phpConfig, phpDb.username, phpDb.password, false)

            request = Mockito.mock()
            Mockito.`when`(request.method).thenReturn("POST")
            Mockito.`when`(request.getHeader("Content-Type")).thenReturn("application/x-www-form-urlencoded")
            Mockito.`when`(request.headerNames).thenReturn(NullEnumeration.create<Any>())

            dbFile = File.createTempFile(System.currentTimeMillis().toString(), "ptd2-test.db")
            dbFile.deleteOnExit()

            val config = HikariConfig()
            config.jdbcUrl = "jdbc:sqlite:$dbFile"
            val dataSource = HikariDataSource(config)

            val databaseServer = dataSource.connection.metaData.databaseProductName.lowercase(Locale.ENGLISH)
            SQLMigration(dataSource.connection, getCorrectFile("db/migration/ptd2/$databaseServer", true))

            database = Database.connect(dataSource)

            // Ensure 1 account is created for tests, reduces redundancy
            createAccount()
        }

//        @JvmStatic
//        @AfterAll
//        fun afterAll() {
//            // Not sure how
//            dbFile.delete()
//        }

        fun createAccount(email: String = "test", password: String = "test"): Pair<String, String> {
            val body = "Action=createAccount&Email=$email&Pass=$password&ver=152"
            val old = runPhpFile(body = body)
            val new = runNew(body = body)

            return old to new
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

        fun runPhpFile(file: File, body: String): String {
            val page = quercus.parse(FilePath(file.path))

            Mockito.`when`(request.inputStream).thenReturn(body.byteInputStream())
            val env = Env(quercus, page, WriteStream(StringWriter()), request, null)
            env.start()

            env.execute()
            //page.execute(env)

            return String(env.out.buffer.sliceArray(0..<env.out.position.toInt()))
        }

        private fun runPhpFile(file: String = "ptd2_save_12.php", body: String): String {
            return runPhpFile(File(directory, file), body)
        }
    }

    @Test
    fun createAccountTest() {
        // TODO: Fix, test only works half the time, not so consistently?
        // First run, success, second run, taken
        for (i in 1..2) {
            val (old, new) = createAccount("test2", "test2")

            assertEquals(old, new)
        }
    }

    @Test
    fun loadAccount() {
        val body = "Action=loadAccount&Email=test&Pass=test&ver=152"
        val old = runPhpFile(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadAccountNotFound() {
        val body = "Action=loadAccount&Email=notFound&Pass=notFound&ver=152"
        val old = runPhpFile(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadStory() {
        val body = "Action=loadStory&Email=test&Pass=test&ver=152"
        val old = runPhpFile(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun loadStoryProfile() {
        // Shouldn't really change anything but why not
        for (i in 1..3) {
            val body = "Action=loadStoryProfile&Email=test&Pass=test&ver=152&whichProfile=$i"
            val old = runPhpFile(body = body)
            val new = runNew(body = body)

            assertEquals(old, new)
        }
    }

    @Test
    fun saveStory() {
        // Shouldn't really change anything but why not
        for (i in 1..3) {
            val body = "extra=Save%3Dtrue%26NewGameSave%3Dtrue%26Nickname%3DGold%26Color%3D1%26Gender%3D1%26MapSave%3Dtrue%26MapLoc%3D1%26MapSpot%3D1%26CS%3Dnull" +
                    "&dextra5=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                    "&Email=test" +
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
                    "&whichProfile=$i" +
                    "&dextra1=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" +
                    "&extra4=yqym" +
                    "&Pass=test"

            val old = runPhpFile(body = body)
            val new = runNew(body = body)

            assertEquals(old, new)
        }
    }

    @Test
    fun loadOneOnOne() {
        val body = "Action=load1on1&Email=test&Pass=test&ver=152"
        val old = runPhpFile(body = body)
        val new = runNew(body = body)

        assertEquals(old, new)
    }

    @Test
    fun saveOneOnOne() {
        for (i in 1..3) {
            val body = "extra=yrwwmyy" +
                    "&Email=test" +
                    "&ver=152" +
                    "&Action=save1on1" +
                    "&whichProfile=$i" +
                    "&extra2=yqym" +
                    "&Pass=test"
            val old = runPhpFile(body = body)
            val new = runNew(body = body)

            assertEquals(old, new)
        }
    }

    @Test
    fun deleteOneOnOne() {
        for (i in 1..3) {
            val body = "Email=test" +
                    "&Action=delete1on1" +
                    "&ver=152" +
                    "&whichProfile=$i" +
                    "&Pass=test"
            val old = runPhpFile(body = body)
            val new = runNew(body = body)

            assertEquals(old, new)
        }
    }
}