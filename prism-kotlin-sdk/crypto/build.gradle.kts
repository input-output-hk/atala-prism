plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.chromaticnoise.multiplatform-swiftpackage") version "2.0.3"
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
        moduleName = "crypto"
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.library()
        useCommonJs()

        compilations["main"].packageJson {
            version = "0.1.0"
        }

        compilations["test"].packageJson {
            version = "0.1.0"
        }
    }

    ios("ios") {
        binaries.framework {
            baseName = "Crypto"
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

        compilations.getByName("main") {
            cinterops.create("secp256k1") {
                val interopTask = tasks[interopProcessingTaskName]
                includeDirs.headerFilterOnly("$rootDir/Secp256k1/include")
            }
        }
    }

    multiplatformSwiftPackage {
        packageName("Crypto")
        swiftToolsVersion("5.3")
        targetPlatforms {
            iOS { v("13") }
        }
        outputDirectory(File(rootDir, "../prism-ios-wallet/CryptoSDK"))
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.ionspin.kotlin:bignum:0.3.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            kotlin.srcDir("src/commonJvmAndroidMain/kotlin")
            dependencies {
                implementation("com.madgag.spongycastle:prov:1.58.0.0")
                implementation("org.bitcoinj:bitcoinj-core:0.15.10")
                api("com.google.guava:guava:30.1-jre")
            }
        }
        val androidTest by getting {
            kotlin.srcDir("src/commonJvmAndroidTest/kotlin")
            resources.srcDir("src/commonJvmAndroidTest/resources")
            dependencies {
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
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")

                implementation(npm("hash.js", "1.1.7", generateExternals = true))
                implementation(npm("elliptic", "6.5.3"))
                implementation(npm("bip32", "2.0.6"))
                implementation(npm("bip39", "3.0.3"))

                // Polyfill dependencies
                implementation(npm("stream-browserify", "3.0.0"))
                implementation(npm("buffer", "6.0.3"))
            }
        }
        val jsTest by getting
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
}

android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(29)
    }
}
