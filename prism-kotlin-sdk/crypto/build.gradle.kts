plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    `maven-publish`
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    js(IR) {
        nodejs()
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
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.ionspin.kotlin:bignum:0.2.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.madgag.spongycastle:prov:1.58.0.0")
                implementation("org.bitcoinj:bitcoinj-core:0.15.8")
                api("com.google.guava:guava:30.1-jre")
            }
        }
        val jvmTest by getting {
            dependencies {
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
        val iosMain by getting
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

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }
}
