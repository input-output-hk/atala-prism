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
    iosX64("ios") {
        binaries.all {
            // Linker options required to link to libsecp256k1.
            linkerOpts("-L../credentials-verification-iOS/Pods/BitcoinKit/Libraries/secp256k1/lib", "-lsecp256k1")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.ionspin.kotlin:bignum:0.2.3")
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
        val iosMain by getting
        val iosTest by getting
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

        // This is a self-written Pod hosted in a custom spec repository (see definition below):
        // https://github.com/itegulov/Specs/blob/main/bitcoin-secp256k1/0.1.0/bitcoin-secp256k1.podspec.json
        pod("bitcoin-secp256k1", version = "0.1.0")

        specRepos {
            url("https://github.com/itegulov/Specs")
        }
    }
}

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }
}
