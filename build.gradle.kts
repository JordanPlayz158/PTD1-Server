import java.io.ByteArrayOutputStream

val ktor_version: String by project
val exposed_version: String by project
val junit_version: String by project
val testcontainers_version: String by project

plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.8.21"
    id("io.ktor.plugin") version "2.3.9" // Cannot use ktor_version variable as version unfortunately

    id("org.graalvm.buildtools.native") version "0.10.0"

    id("maven-publish")
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
            "NO_EXISTING_TAGS"
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

group = "xyz.jordanplayz158.ptd1"
version = "$tag${if(!isRelease) "-$commit" else ""}"



// Build related
tasks.jar {
    archiveFileName = "${archiveBaseName.get()}-${archiveVersion.get()}-nodeps.${archiveExtension.get()}"
}

tasks.shadowJar {
    archiveFileName = "${archiveBaseName.get()}-${archiveVersion.get()}.${archiveExtension.get()}"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
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

graalvmNative {
    agent {
        defaultMode = "standard"
    }

    binaries {
        all {
            resources.autodetect()
        }

        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            })
        }

//        named("main") {
//            //buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory,ch.qos.logback")
//        }
    }

    //metadataRepository.enabled = true
}

tasks.metadataCopy {
    inputTaskNames.add("run")
    outputDirectories.add("src/main/resources/META-INF/native-image")
}



// Test related
tasks.compileTestKotlin {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}



repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    implementation("ch.qos.logback:logback-classic:1.5.3")

    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-thymeleaf:$ktor_version")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")

    implementation("io.ktor:ktor-server-webjars:$ktor_version")
    implementation("org.webjars:bootstrap:5.3.3")

    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    //implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
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



publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
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