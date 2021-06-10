import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("org.openjfx.javafxplugin") version "0.0.9"
}

group = "io.github.gaeqs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "14.0.2.1"
    modules("javafx.base", "javafx.controls", "javafx.media", "javafx.graphics", "javafx.fxml", "javafx.swing")
}

dependencies {
    testImplementation(kotlin("test-junit"))

    implementation(files("lib/JAMS-0.2.jar"))
    implementation(group = "no.tornado", name = "tornadofx", version = "1.7.20")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("JAMS-0.2.jar")
    dependencies {
        exclude(dependency("org.openjfx::"))
    }
}