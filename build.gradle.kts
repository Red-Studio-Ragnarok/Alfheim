import org.jetbrains.gradle.ext.settings
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.3.24"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
    id("com.matthewprenger.cursegradle") version "1.4.0"
    id("com.github.gmazzo.buildconfig") version "4.1.2"
    id("io.freefair.lombok") version "8.4"
}

group = "dev.redstudio"
version = "1.1-Dev-2" // Versioning must follow Ragnarök versioning convention: https://shor.cz/ragnarok_versioning_convention

val id = project.name.lowercase()
val plugin = "asm.${project.name}Plugin"

val redCoreVersion = "MC-1.7-1.12-" + "0.5-Dev-5"

minecraft {
    mcVersion = "1.12.2"
    username = "Desoroxxx"
    extraRunJvmArguments = listOf("-Dforge.logging.console.level=debug", "-Dfml.coreMods.load=${project.group}.${id}.${plugin}", "-Dmixin.hotSwap=true", "-Dmixin.checks.mixininterfaces=true", "-Dmixin.debug.export=true")
    javaToolchain.get().vendor.set(JvmVendorSpec.ADOPTIUM)
}

configurations {
    create("sources") // Define a configuration to download and attach sources
}

repositories {
    maven {
        name = "Cleanroom"
        url = uri("https://maven.cleanroommc.com")
    }

    maven {
        name = "SpongePowered"
        url = uri("https://repo.spongepowered.org/maven")
    }

    maven {
        name = "Curse Maven"
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }

    ivy {
        name = "Red Studio GitHub Releases"
        url = uri("https://github.com/")

        patternLayout {
            artifact("[organisation]/[module]/releases/download/[revision]/[module]-[revision](-[classifier]).[ext]")
        }

        metadataSources {
            artifact()
        }
    }
}

dependencies {
    implementation("Red-Studio-Ragnarok", "Red-Core", redCoreVersion)
    add("sources", "Red-Studio-Ragnarok:Red-Core:${redCoreVersion}:sources@jar")

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
            "FMLAT" to "${id}_at.cfg",
            "FMLCorePlugin" to "${project.group}.${id}.${plugin}",
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

