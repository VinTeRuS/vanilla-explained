plugins {
    id("dev.kikugie.loom-back-compat")
    id("mod-conventions")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    testRuntimeOnly(files(sourceSets.main.get().compileClasspath))
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run/${sc.current.project}")
        jvmArguments.add("-Dmixin.debug.export=true")
        if (System.getenv("VE_SMOKE") != null) jvmArguments.add("-Dve.smoke=true")
        if (System.getenv("VE_GALLERY") != null) jvmArguments.add("-Dve.gallery=true")
    }
}

tasks {
    processResources {
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta")
    }

    named<Copy>("buildAndCollect") {
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    }
}

// In-game client tests run on the newest version only.
if (sc.current.version == "26.3") {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "vanillaexplained-gametest"
            enableGameTests = false
            eula = true
        }
    }
    sourceSets.named("gametest") {
        java.setSrcDirs(listOf(rootProject.file("src/gametest/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/gametest/resources")))
    }
}
