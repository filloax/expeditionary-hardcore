pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
            content {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val modid: String by settings

rootProject.name = modid

listOf(
    "base",
    "neoforge",
    "fabric",
).forEach { include(it) }

// until published properly, use apibalego from local workspace
includeBuild("../apibalego") {
    dependencySubstitution {
        substitute(module("com.github.filloax.apibalego:apibalego-base")).using(project(":base"))
        substitute(module("com.github.filloax.apibalego:apibalego-fabric")).using(project(":fabric"))
        substitute(module("com.github.filloax.apibalego:apibalego-neoforge")).using(project(":neoforge"))
    }
}
