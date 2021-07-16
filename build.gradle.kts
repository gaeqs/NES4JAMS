import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")

    implementation(files("lib/JAMS.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation(group = "no.tornado", name = "tornadofx", version = "1.7.20")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("JAMS.jar")
    dependencies {
        exclude(dependency("org.openjfx::"))
    }
}

task("shadowAndRun") {
    dependsOn("shadowJar")
    doLast {
        javaexec {
            main = "-jar"
            args = listOf("lib/JAMS.jar", "-loadPlugin", "build/libs/NES4JAMS-$version.jar")
        }
    }
}

task("shadowAndRunWithDebugger") {
    dependsOn("shadowJar")

    doLast {
        javaexec {
            debugOptions {
                enabled.value(true)
            }
            main = "-jar"
            args = listOf("lib/JAMS.jar", "-loadPlugin", "build/libs/NES4JAMS-$version.jar")
        }
    }
    println(file("lib/JAMS").absolutePath)
}