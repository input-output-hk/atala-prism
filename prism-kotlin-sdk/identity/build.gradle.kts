plugins {
    kotlin("multiplatform")
    `maven-publish`
}
val pbandkVersion: String by rootProject.extra

dependencies {
    commonMainImplementation("com.github.h0tk3y.betterParse:better-parse:0.4.1")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
    }
    ios("ios") {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
        binaries.all {
            // Linker options required to link to libsecp256k1.
            linkerOpts("-L../credentials-verification-iOS/Pods/BitcoinKit/Libraries/secp256k1/lib", "-lsecp256k1")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":protos"))
                api(project(":crypto"))
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
                api("org.bitcoinj:bitcoinj-core:0.15.8")
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
}

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }
}
