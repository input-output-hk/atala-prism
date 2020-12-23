import com.palantir.gradle.gitversion.VersionDetails

plugins {
    kotlin("multiplatform") version "1.4.21"
    kotlin("native.cocoapods") version "1.4.21"
    id("maven-publish")
    id("com.palantir.git-version") version "0.12.3"
}
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val pbandkVersion by extra("0.9.0-SNAPSHOT")

group = "io.iohk.atala.prism"
version = "0.1-" + versionDetails().gitHash.substring(0, 8)

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://dl.bintray.com/itegulov/maven") }
}

dependencies {
    // This is our own fork of better-parse that is published to bintray/itegulov.
    // The source code repository is https://github.com/itegulov/better-parse.
    // TODO: Replace with the mainstream version once https://github.com/h0tk3y/better-parse/pull/33
    //       is merged.
    commonMainImplementation("com.github.h0tk3y.betterParse:better-parse-ng:0.5.0-M1")
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
    iosX64("ios") {
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
            kotlin.srcDir("${project(":protos").buildDir}/generated/source/proto/main/kotlin")
            dependencies {
                implementation("com.ionspin.kotlin:bignum:0.2.3")
                implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
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

    project(":protos").tasks
        .matching { it.name == "generateProto" }
        .all {
            val task = compileKotlinMetadata.get()
            task.dependsOn(this)
            task.kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
}
