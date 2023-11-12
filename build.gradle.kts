import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

val ktor_version: String by project
val flyway_version: String by project
val exposed_version: String by project
val junit_version: String by project
val testcontainers_version: String by project

plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.9.27"
    id("maven-publish")
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.8.21"
    // Cannot use ktor_version variable as version unfortunately
    id("io.ktor.plugin") version "2.3.4"
}

var tagEnv: String? = System.getenv("CI_COMMIT_TAG")
val tag = if(tagEnv === null) {
    ByteArrayOutputStream().use { outputStream ->
        try {
            project.exec {
                executable("git")
                args("describe", "--tags", "--abbrev=0")
                standardOutput = outputStream
            }
            outputStream.toString().trim()
        } catch(e: Exception) {
            ""
        }
    }
} else {
    "$tagEnv"
}



val isRelease = tagEnv !== null

var commitEnv: String? = System.getenv("CI_COMMIT_SHORT_SHA")
val commit = if(commitEnv === null) {
    ByteArrayOutputStream().use { outputStream ->
        project.exec {
            executable("git")
            args("rev-parse", "--short", "HEAD")
            standardOutput = outputStream
        }
        outputStream.toString().trim()
    }
} else {
    commitEnv
}



group = "xyz.jordanplayz158"
version = "$tag${if(!isRelease) "-$commit" else ""}"
archivesName

tasks.withType<Jar> {
    archiveFileName = "${archiveBaseName.get()}-${archiveVersion.get()}-nodeps.${archiveExtension.get()}"
}

tasks.withType<ShadowJar> {
    this.archiveFileName = "${archiveBaseName.get()}-${archiveVersion.get()}.${archiveExtension.get()}"
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    implementation("ch.qos.logback:logback-classic:1.4.8")

    implementation("io.ktor:ktor-server-jetty:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker:$ktor_version")

    implementation("io.ktor:ktor-server-webjars:$ktor_version")
    implementation("org.webjars:bootstrap:5.3.0")

    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("com.google.guava:guava:32.1.2-jre")

    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.2.0")
    implementation("com.mysql:mysql-connector-j:8.1.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-mysql:$flyway_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")


    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:$junit_version"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")

    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers_version")
    testImplementation("org.testcontainers:mariadb:$testcontainers_version")
    testImplementation("org.testcontainers:mysql:$testcontainers_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}

application {
    // is NOT included in default jvm args as GraalVM seems to auto-generate problematic entries in reflect-config.json
    // and misses one entry in resource-config.json. Issues https://github.com/oracle/graal/issues/4797#issuecomment-1711071349 and
    // https://youtrack.jetbrains.com/issue/KTOR-6069/Ktor-Native-Build-Issue#focus=Comments-27-7689345.0-0 respectively so these
    // files will be included in Git/VCS as a result (rather than auto-generating in GitLab CI)
    //applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true", "-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("xyz.jordanplayz158.ptd1.server.PTD1ServerKt")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

graalvmNative {
    binaries {
        all {
            resources.autodetect()
        }

//        named("main") {
//            //buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory,ch.qos.logback")
//        }
    }
}
