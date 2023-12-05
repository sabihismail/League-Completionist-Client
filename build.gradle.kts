import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "me.sabihismail"
version = "1.0.0"

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    // id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

//javafx {
//    version = "1.8"
//    modules("javafx.controls", "javafx.graphics")
//}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    // maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    // maven { url = uri("https://nexus.stirante.com/repository/maven-snapshots/") }
}

dependencies {
    implementation("com.github.stelar7", "R4J", "2.2.9")
    implementation("com.github.stirante", "lol-client-java-api", "-SNAPSHOT")  // "1.2.8-SNAPSHOT")
    implementation("no.tornado", "tornadofx", "1.7.20")
    implementation("org.apache.commons", "commons-lang3", "3.12.0")
    implementation("org.junit.jupiter", "junit-jupiter", "5.9.2")
    implementation("com.squareup.okhttp3", "okhttp", "4.11.0")
    implementation("org.xerial", "sqlite-jdbc", "3.40.0.0")
    implementation("org.slf4j", "slf4j-nop", "2.0.9")

    val exposedVersion = "0.45.0"
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
        doLast {
            val file = File("config.json")
            val json = groovy.json.JsonSlurper().parseText(file.readText()) as Map<*, *>
            val exportDirectory = json.getOrDefault("exportDirectory", System.getProperty("user.home")) as String

            copy {
                from("build/libs/LoL-Mastery-Box-Client-1.0.0-all.jar")
                into(exportDirectory)
            }
        }
    }

    test {
        useJUnitPlatform()
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
