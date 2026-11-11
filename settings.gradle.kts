pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://files.minecraftforge.net/maven/") { name = "Minecraft Forge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("net.neoforged.moddev.legacyforge") version "2.0.147" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.20.1", "fabric", "forge")
        match("1.20.4", "fabric")
        match("1.20.6", "fabric")
        match("1.21.1", "fabric")
        match("1.21.4", "fabric")
        match("1.21.5", "fabric")
        match("1.21.8", "fabric")
        match("1.21.10", "fabric")
        match("1.21.11", "fabric")
        match("26.1.2", "fabric")
        match("26.2", "fabric")
        match("26.3", "fabric")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "vanilla-explained"
