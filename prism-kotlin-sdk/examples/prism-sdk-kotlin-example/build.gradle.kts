import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    application
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

// Replace with the current version
val prismVersion = "0.1.0-941fa0d2"

group = "io.iohk.atala.prism.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven("https://plugins.gradle.org/m2/")
    maven("https://dl.bintray.com/itegulov/maven")
    maven("https://kotlin.bintray.com/kotlinx/")
    maven("https://dl.bintray.com/acinq/libs")
}

dependencies {
    listOf("protos-jvm", "crypto-jvm", "identity-jvm", "credentials-jvm", "extras-jvm")
        .map { "io.iohk.atala.prism:$it:$prismVersion" }
        .map {
            implementation(it) {
                // This is excluded to avoid issues when the PRISM SDK is only published to the JVM,
                // for some reason, identity-jvm doesn't depend on crypto-jvm but crypto which isn't published.
                exclude("io.iohk.atala.prism")
            }
        }
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "io.iohk.atala.prism.example.MainKt"
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")

ktlint {
    verbose.set(true)
    outputToConsole.set(true)

    // Exclude generated proto classes
    filter {
        exclude { element ->
            element.file.path.contains("generated/") or
                element.file.path.contains("externals/")
        }
    }
}
