import com.palantir.gradle.gitversion.VersionDetails

plugins {
    kotlin("multiplatform") version "1.4.21" apply false
    kotlin("plugin.serialization") version "1.4.21" apply false
    kotlin("native.cocoapods") version "1.4.21" apply false
    id("com.google.protobuf") version "0.8.14" apply false
    id("com.palantir.git-version") version "0.12.3"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val pbandkVersion by extra("0.10.0-M1")

allprojects {
    group = "io.iohk.atala.prism"
    version = "0.1-" + versionDetails().gitHash.substring(0, 8)

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://plugins.gradle.org/m2/")
        maven { setUrl("https://dl.bintray.com/itegulov/maven") }
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        verbose.set(true)
        outputToConsole.set(true)

        // Exclude generated proto classes
        filter {
            exclude { element -> element.file.path.contains("generated/") }
        }
    }
}
