plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    `maven-publish`
    id("com.android.library")
}

dependencies {
    commonMainImplementation("com.soywiz.korlibs.krypto:krypto:2.0.6")
}

kotlin {
    android {
        publishAllLibraryVariants()
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChrome()
                }
            }
        }
        binaries.executable()
        useCommonJs()

        compilations["main"].packageJson {
            version = "0.1.0"
        }

        compilations["test"].packageJson {
            version = "0.1.0"
        }
    }
    ios("ios") {
        binaries.all {
            // Linker options required to link to libsecp256k1.
            linkerOpts("-L$rootDir/crypto/build/cocoapods/synthetic/IOS/crypto/Pods/Secp256k1Kit.swift/Secp256k1Kit/Libraries/lib", "-lsecp256k1")
        }

        // Facade to SwiftCryptoKit
        val platform = when (name) {
            "ios" -> "iphonesimulator"
            "iosX64" -> "iphonesimulator"
            "iosArm64" -> "iphoneos"
            else -> error("Unsupported target $name.")
        }
        compilations.getByName("main") {
            cinterops.create("SwiftCryptoKit") {
                val interopTask = tasks[interopProcessingTaskName]
                interopTask.dependsOn(":SwiftCryptoKit:build${platform.capitalize()}")
                includeDirs.headerFilterOnly("$rootDir/SwiftCryptoKit/build/Release-$platform/include")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.ionspin.kotlin:bignum:0.2.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            kotlin.srcDir("src/commonJvmAndroidMain/kotlin")
            dependencies {
                implementation("com.madgag.spongycastle:prov:1.58.0.0")
                implementation("org.bitcoinj:bitcoinj-core:0.15.8")
                api("com.google.guava:guava:30.1-jre")
            }
        }
        val androidTest by getting {
            kotlin.srcDir("src/commonJvmAndroidTest/kotlin")
            resources.srcDir("src/commonJvmAndroidTest/resources")
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.robolectric:android-all:10-robolectric-5803371")
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("src/commonJvmAndroidMain/kotlin")
            dependencies {
                implementation("org.bouncycastle:bcprov-jdk15on:1.68")
                implementation("org.bitcoinj:bitcoinj-core:0.15.10")
                api("com.google.guava:guava:30.1-jre")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/commonJvmAndroidTest/kotlin")
            resources.srcDir("src/commonJvmAndroidTest/resources")
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("hash.js", "1.1.7", generateExternals = true))
                implementation(npm("elliptic", "6.5.3"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val iosMain by getting {
            dependencies {
                api("fr.acinq.bitcoin:bitcoin-kmp:0.7.0")
            }
        }
        val iosTest by getting

        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            languageSettings.useExperimentalAnnotation("kotlin.js.ExperimentalJsExport")
        }
    }

    publishing {
        repositories {
            mavenLocal()
        }
    }

    cocoapods {
        // Configure fields required by CocoaPods.
        summary = "Atala PRISM Multiplatform SDK"
        homepage = "https://atalaprism.io/"

        ios.deploymentTarget = "13.0"

        pod("Secp256k1Kit.swift", version = "1.1", moduleName = "Secp256k1Kit")
    }
}

android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(29)
    }
}

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }
}
