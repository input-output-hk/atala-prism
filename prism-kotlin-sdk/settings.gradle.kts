include(":protos")
include(":crypto")
include(":identity")
include(":credentials")
include(":docs")
include(":generator")

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
    }

    dependencies {
        classpath("io.arrow-kt:arrow-ank-gradle:0.11.0")
    }
}

rootProject.name = "prism-kotlin-sdk"
