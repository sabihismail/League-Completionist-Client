import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "me.sabihismail"
version = "1.0.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.stelar7", "R4J", "2.2.5")
    implementation("com.github.stirante", "lol-client-java-api", "-SNAPSHOT")
    implementation("no.tornado", "tornadofx", "1.7.20")
    implementation("org.apache.commons", "commons-lang3", "3.12.0")
    implementation("org.junit.jupiter", "junit-jupiter", "5.9.1")

    val exposedVersion = "0.41.1"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

    implementation("org.xerial", "sqlite-jdbc", "3.39.4.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.4.1")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}

tasks {
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
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}