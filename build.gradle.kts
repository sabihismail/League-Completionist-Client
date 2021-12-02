import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.sabihismail"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.5.10"
    id("org.openjfx.javafxplugin") version "0.0.10"
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("com.github.stirante:lol-client-java-api:1.2.3")
}

javafx {
    modules("javafx.controls", "javafx.graphics")
}

application {
    mainClass.set("MainKt")
}