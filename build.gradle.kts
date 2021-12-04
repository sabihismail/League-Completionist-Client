import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks.test {
    useJUnitPlatform()
}

group = "me.sabihismail"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("com.github.stirante:lol-client-java-api:1.2.3")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}