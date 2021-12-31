plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.vtence.console"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("GameKt")
}