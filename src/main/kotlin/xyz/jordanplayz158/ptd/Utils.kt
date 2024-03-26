package xyz.jordanplayz158.ptd

import com.zaxxer.hikari.HikariConfig
import io.github.cdimascio.dotenv.Dotenv
import java.io.File

fun getCorrectFile(path: String, developmentMode: Boolean) : File {
    return when (developmentMode) {
        // For development, it is nicer to bypass the folders and allow changes to
        //  resources folder directly
        true -> File(object {}.javaClass.getResource("/$path")!!.toURI())
        false -> File(path)
    }
}

fun databaseConfig(dotenv: Dotenv) : HikariConfig {
    val config = HikariConfig()

    config.jdbcUrl = dotenv["DATABASE_URL"]
    config.username = dotenv["DATABASE_USERNAME"]
    config.password = dotenv["DATABASE_PASSWORD"]

    val databaseDriver = dotenv["DATABASE_DRIVER"]
    if(databaseDriver !== null) config.driverClassName = databaseDriver

    config.addDataSourceProperty("cachePrepStmts", dotenv["DATABASE_CACHE_PREP_STMTS", "true"])
    config.addDataSourceProperty("prepStmtCacheSize", dotenv["DATABASE_PREP_STMT_CACHE_SIZE", "375"])
    config.addDataSourceProperty("prepStmtCacheSqlLimit", dotenv["DATABASE_PREP_STMT_CACHE_SQL_LIMIT", "2048"])
    config.addDataSourceProperty("useServerPrepStmts", dotenv["DATABASE_USE_SERVER_PREP_STMTS", "true"])

    return config
}