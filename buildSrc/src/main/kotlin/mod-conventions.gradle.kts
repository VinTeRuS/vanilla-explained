import dev.kikugie.stonecutter.build.StonecutterBuildExtension

plugins {
    `java-library`
}

val sc = extensions.getByType<StonecutterBuildExtension>()
val modId = sc.properties.get<String>("mod.id")
val modVersion = sc.properties.get<String>("mod.version")
val loader = sc.current.project.substringAfter('-')

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

val fabricEnvironments = mapOf("both" to "*", "client" to "client", "server" to "server")
val environment = sc.properties.get<String>("mod.environment")
val fabricEnvironment = requireNotNull(fabricEnvironments[environment]) {
    "mod.environment must be one of ${fabricEnvironments.keys.toList()}, got '$environment'"
}

group = sc.properties.get<String>("mod.group")
version = "$modVersion+${sc.current.version}"
base.archivesName = "${sc.properties.get<String>("mod.artifact")}-$loader"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${sc.properties.get<String>("deps.jupiter")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(files(sourceSets.main.get().compileClasspath))
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    processResources {
        val modMetadata = buildMap {
            put("id", modId)
            put("name", sc.properties.get<String>("mod.name"))
            put("version", modVersion)
            put("description", sc.properties.get<String>("mod.description"))
            put("author", sc.properties.get<String>("mod.author"))
            put("sources", sc.properties.get<String>("mod.sources"))
            put("environment", fabricEnvironment)
            put("minecraft", sc.properties.get<String>("mod.mc_compat"))
            put("java_version", requiredJava.majorVersion)
            put("mixin_compatibility", "JAVA_${requiredJava.majorVersion}")
            put("icon_key", if (sc.current.parsed >= "26.2") "iconFile" else "logoFile")
            sc.properties.getOrNull<String>("deps.fabric_loader")?.let { fabricLoader ->
                put("fabric_loader", requireNotNull(Regex("\\d+\\.\\d+").find(fabricLoader)) {
                    "deps.fabric_loader must start with major.minor, got $fabricLoader"
                }.value)
            }
            sc.properties.getOrNull<String>("mod.neoforge_compat")?.let { put("neoforge", it) }
        }

        inputs.properties(modMetadata)
        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
            expand(modMetadata)
        }
    }

    withType<Jar>().configureEach {
        val licenseName = "LICENSE-$modId"
        inputs.property("mod_id", modId)
        from(rootProject.file("LICENSE.md")) { rename { licenseName } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to build/libs/<mod version>/"
        inputs.property("version", modVersion)
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}
