include(":SwiftCryptoKit")
include(":protos")
include(":protosLib")
include(":crypto")
include(":identity")
include(":credentials")
include(":docs")
include(":generator")
include(":integration-tests")

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "binary-compatibility-validator" -> {
                    useModule("org.jetbrains.kotlinx:binary-compatibility-validator:${requested.version}")
                }
                "com.android.application", "com.android.library" -> {
                    useModule("com.android.tools.build:gradle:${requested.version}")
                }
            }
        }
    }
}

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
