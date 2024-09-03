import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "me.sabihismail"
version = "0.4.2"

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    // id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.github.ben-manes.versions") version "0.51.0"
    application
}

//javafx {
//    version = "1.8"
//    modules("javafx.controls", "javafx.graphics")
//}

repositories {
    mavenCentral()
    maven { url = uri("https://nexus.stirante.com/repository/maven-snapshots/") }
}

dependencies {
    implementation("com.stirante", "lol-client-java-api", "1.2.11-SNAPSHOT")  // "1.2.8-SNAPSHOT")
    implementation("no.tornado", "tornadofx", "1.7.20")
    implementation("org.apache.commons", "commons-lang3", "3.14.0")
    implementation("org.junit.jupiter", "junit-jupiter", "5.9.2")
    implementation("com.squareup.okhttp3", "okhttp", "4.11.0")
    implementation("org.xerial", "sqlite-jdbc", "3.46.0.0")
    // implementation("org.slf4j", "slf4j-nop", "2.0.9")

    val exposedVersion = "0.52.0"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.6.2")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    shadowJar {
        val jarFileName = "League-Completionist-Client.jar"

        archiveFileName.set(jarFileName)

        doLast {
            val file = File("config.json")
            val json = groovy.json.JsonSlurper().parseText(file.readText()) as Map<*, *>
            val exportDirectory = json.getOrDefault("exportDirectory", System.getProperty("user.home")) as String

            copy {
                from("build/libs/${jarFileName}")
                into(exportDirectory)
            }
        }
    }

    test {
        useJUnitPlatform()
    }

    compileKotlin.get().kotlinOptions {
        jvmTarget = "1.8"
    }

    compileTestKotlin.get().kotlinOptions {
        jvmTarget = "1.8"
    }
}
