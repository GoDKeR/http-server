plugins {
    kotlin("jvm") version "2.0.0"
}

group = "org.godker.http.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-network:2.3.12")
}

tasks.test {
    useJUnitPlatform()
}