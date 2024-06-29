import org.jetbrains.gradle.ext.settings
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.3.34"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
    id("io.freefair.lombok") version "8.6"
}

group = "dev.redstudio"
version = "1.4-Dev-1" // Versioning must follow Ragnarök versioning convention: https://github.com/Red-Studio-Ragnarok/Commons/blob/main/Ragnar%C3%B6k%20Versioning%20Convention.md

val id = project.name.lowercase()
val plugin = "${project.group}.${id}.asm.${project.name}Plugin"

val redCoreVersion = "MC-1.8-1.12-" + "0.6-Dev-5"

minecraft {
    mcVersion = "1.12.2"
    username = "Desoroxxx"
    extraRunJvmArguments = listOf("-Dforge.logging.console.level=debug", "-Dfml.coreMods.load=${plugin}", "-Dmixin.hotSwap=true", "-Dmixin.checks.mixininterfaces=true", "-Dmixin.debug.export=true")
}

repositories {
    maven {
        name = "Cleanroom"
        url = uri("https://maven.cleanroommc.com")
    }

    listOf("release", "beta", "dev").forEach { repoType ->
        maven {
            name = "Red Studio - ${repoType.capitalize()}"
            url = uri("https://repo.redstudio.dev/$repoType")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Curse Maven"
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    implementation("dev.redstudio", "Red-Core", redCoreVersion)

    implementation(rfg.deobf("curse.maven:dynamic-lights-227874:2563244"))

    annotationProcessor("org.ow2.asm", "asm-debug-all", "5.2")
    annotationProcessor("com.google.guava", "guava", "32.1.2-jre")
    annotationProcessor("com.google.code.gson", "gson", "2.8.9")

    val mixinBooter: String = modUtils.enableMixins("zone.rong:mixinbooter:8.6", "mixins.${id}.refmap.json") as String
    api(mixinBooter) {
        isTransitive = false
    }
    annotationProcessor(mixinBooter) {
        isTransitive = false
    }
}

buildConfig {
    packageName("${project.group}.${id}")
    className("ProjectConstants")
    documentation.set("This class defines constants for ${project.name}.")

    useJavaOutput()
    buildConfigField("String", "ID", provider { """"${id}"""" })
    buildConfigField("String", "NAME", provider { """"${project.name}"""" })
    buildConfigField("String", "VERSION", provider { """"${project.version}"""" })
    buildConfigField("org.apache.logging.log4j.Logger", "LOGGER", "org.apache.logging.log4j.LogManager.getLogger(NAME)")
    buildConfigField("dev.redstudio.redcore.logging.RedLogger", "RED_LOGGER", """new RedLogger(NAME, "https://linkify.cz/AlfheimBugReport", LOGGER)""")
}

idea {
    module {
        inheritOutputDirs = true

        excludeDirs = setOf(
                file(".github"), file(".gradle"), file(".idea"), file("build"), file("gradle"), file("run")
        )
    }

    project {
        settings {
            jdkName = "1.8"
            languageLevel = IdeaLanguageLevel("JDK_1_8")

            runConfigurations {
                create("Client", Gradle::class.java) {
                    taskNames = setOf("runClient")
                }
                create("Server", Gradle::class.java) {
                    taskNames = setOf("runServer")
                }
                create("Obfuscated Client", Gradle::class.java) {
                    taskNames = setOf("runObfClient")
                }
                create("Obfuscated Server", Gradle::class.java) {
                    taskNames = setOf("runObfServer")
                }
                create("Vanilla Client", Gradle::class.java) {
                    taskNames = setOf("runVanillaClient")
                }
                create("Vanilla Server", Gradle::class.java) {
                    taskNames = setOf("runVanillaServer")
                }
            }
        }
    }
}

// Set the toolchain version to decouple the Java we run Gradle with from the Java used to compile and run the mod
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    withSourcesJar() // Generate sources jar when building and publishing
}

tasks.processResources.configure {
    inputs.property("name", project.name)
    inputs.property("version", project.version)
    inputs.property("id", id)

    filesMatching("mcmod.info") {
        expand(mapOf("name" to project.name, "version" to project.version, "id" to id))
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "ModSide" to "CLIENT",
            "FMLCorePlugin" to "${plugin}",
            "FMLCorePluginContainsFMLMod" to "true",
            "ForceLoadAsMod" to "true"
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx4G")
}

