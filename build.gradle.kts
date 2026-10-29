object BuildConfig {
    val JAVA_VERSION: Int = 25

    val MINECRAFT_VERSION_RANGE: String = ">=26.1" // range: ">=26.1 <27.1"
    val MINECRAFT_PUBLISH_END_OVERRIDE: String? = null
    val MINECRAFT_VERSION: String = "26.1.2"
    val FABRIC_LOADER_VERSION: String = "0.19.5"
    val FABRIC_API_VERSION: String = "0.155.2+26.1.2"

    // https://semver.org/
    var MOD_VERSION: String = "0.1.0"

    val MOD_ID: String = "mannequin-control"
    val MOD_NAME: String = "Mannequin Control"
    val GROUP: String = "io.github.gethigh1227"

    val GITHUB_REPOSITORY: String = "gethigh1227/mannequin-control"
    val MODRINTH_PROJECT_ID: String = ""
    val CURSEFORGE_PROJECT_ID: String = ""

    val MINECRAFT_VERSION_MIN: String
        get() = LOWER_BOUND.version

    val MINECRAFT_PUBLISH_END: String
        get() = MINECRAFT_PUBLISH_END_OVERRIDE
            ?: UPPER_BOUND?.takeIf { it.operator == "<=" }?.version
            ?: "latest"

    private data class Bound(val operator: String, val version: String)

    private val BOUNDS: List<Bound> = MINECRAFT_VERSION_RANGE
        .split(" ")
        .filter { it.isNotBlank() }
        .map { predicate ->
            val operator = predicate.takeWhile { !it.isDigit() }
            Bound(operator, predicate.removePrefix(operator))
        }

    private val LOWER_BOUND: Bound = BOUNDS.first { it.operator.startsWith(">") }
    private val UPPER_BOUND: Bound? = BOUNDS.firstOrNull { it.operator.startsWith("<") }
}

plugins {
    id("java-library")
    id("net.fabricmc.fabric-loom") version("1.17.+")
    id("me.modmuss50.mod-publish-plugin") version("2.2.0")
}

base {
    archivesName = BuildConfig.MOD_ID
}

group = BuildConfig.GROUP
version = createVersionString()

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")
    implementation("net.fabricmc.fabric-api:fabric-api:${BuildConfig.FABRIC_API_VERSION}")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    processResources {
        val propertiesMap = mapOf(
            "version" to version,
            "minecraft_version" to BuildConfig.MINECRAFT_VERSION_RANGE
                .replace("(?<=\\D)-".toRegex(), "."), // fabric snapshot test
            "fabric_loader_version" to BuildConfig.FABRIC_LOADER_VERSION,
            "fabric_api_version" to BuildConfig.FABRIC_API_VERSION
        )
        inputs.properties(propertiesMap)
        filesMatching(listOf("fabric.mod.json")) {
            expand(propertiesMap)
        }
    }

    jar {
        //from("LICENSE")
        destinationDirectory.set(layout.buildDirectory.dir("mods"))
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(BuildConfig.JAVA_VERSION)

    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(BuildConfig.JAVA_VERSION)
}

tasks.test {
    useJUnitPlatform()
}

fun env(name: String, default: String): String = providers.environmentVariable(name)
    .orElse(default)
    .get()

fun envFlag(name: String, default: Boolean): Boolean = env(name, default.toString()).toBoolean()

// https://github.com/modmuss50/mod-publish-plugin
publishMods {
    //dryRun = true

    file = tasks.jar.flatMap { it.archiveFile }
    modLoaders.addAll("fabric", "quilt")

    changelog = env("CHANGELOG_TEXT", "").ifEmpty {
        providers.fileContents(layout.projectDirectory.file("changelog.md")).asText.getOrElse("")
    }

    type = when (env("VERSION_TYPE", "release")) {
        "alpha" -> ALPHA
        "beta" -> BETA
        else -> STABLE
    }

    val publishPlatform = env("PUBLISH_PLATFORM", "all")
    if (publishPlatform !in listOf("all", "modrinth", "curseforge", "github")) {
        error("Unknown PUBLISH_PLATFORM: $publishPlatform")
    }
    val modrinthEnabled = publishPlatform in listOf("all", "modrinth") && BuildConfig.MODRINTH_PROJECT_ID.isNotEmpty()
    val curseforgeEnabled = publishPlatform in listOf("all", "curseforge") && BuildConfig.CURSEFORGE_PROJECT_ID.isNotEmpty()

    if (!envFlag("PUBLISH_ENABLED", true)) {
        println("Publishing is disabled. Set PUBLISH_ENABLED=true to enable.")
        return@publishMods
    }

    val artifactVersion = "mc${BuildConfig.MINECRAFT_VERSION.substringBefore('-')}-${BuildConfig.MOD_VERSION}-fabric"
    val artifactDisplayName = "${BuildConfig.MOD_NAME} ${BuildConfig.MOD_VERSION} for Minecraft ${BuildConfig.MINECRAFT_VERSION}"

    if (curseforgeEnabled) {
        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = BuildConfig.CURSEFORGE_PROJECT_ID
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = BuildConfig.MINECRAFT_PUBLISH_END
            }
            javaVersions.add(JavaVersion.toVersion(BuildConfig.JAVA_VERSION))
            version = artifactVersion
            displayName = artifactDisplayName
            requires("fabric-api")
        }
    }

    if (modrinthEnabled) {
        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = BuildConfig.MODRINTH_PROJECT_ID
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = BuildConfig.MINECRAFT_PUBLISH_END
                includeSnapshots = envFlag("INCLUDE_SNAPSHOTS", false)
            }
            version = artifactVersion
            displayName = artifactDisplayName
            requires("fabric-api")
        }
    }

    github {
        val releaseVersion = "${BuildConfig.MOD_VERSION}+mc${BuildConfig.MINECRAFT_VERSION}"

        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = BuildConfig.GITHUB_REPOSITORY
        commitish = providers.environmentVariable("GITHUB_SHA").orElse("main")
        tagName = releaseVersion
        version = releaseVersion
        displayName = artifactDisplayName
    }
}

tasks.named("publishGithub") {
    mustRunAfter(tasks.matching { it.name == "publishModrinth" || it.name == "publishCurseforge" })
}

fun createVersionString(): String {
    val builder = StringBuilder()

    val isReleaseBuild = project.hasProperty("build.release")
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    if (isReleaseBuild) {
        builder.append(BuildConfig.MOD_VERSION)
    } else {
        builder.append(BuildConfig.MOD_VERSION.substringBefore('-'))
        builder.append("-snapshot")
    }

    builder.append("+mc").append(BuildConfig.MINECRAFT_VERSION)

    if (!isReleaseBuild) {
        if (buildId != null) {
            builder.append("-build.${buildId}")
        } else {
            builder.append("-local")
        }
    }

    return builder.toString()
}
