pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "PTD1-Server"

startParameter.excludedTaskNames.add("distZip")
startParameter.excludedTaskNames.add("distTar")