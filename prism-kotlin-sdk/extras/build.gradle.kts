plugins {
    kotlin("multiplatform")
    id("com.android.library")
    // A small plugin that replaces `kotlin.js.Promise` with plain `Promise` in .d.ts files
    id("com.github.turansky.kfc.definitions") version "3.8.3"
    id("dev.petuska.npm.publish")
}

kotlin {
    android {
        publishAllLibraryVariants()
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
    }
    js(IR) {
        moduleName = "extras"
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
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
        binaries.all {
            // Linker options required to link to libsecp256k1.
            linkerOpts("-L$rootDir/crypto/build/cocoapods/synthetic/IOS/crypto/Pods/Secp256k1Kit.swift/Secp256k1Kit/Libraries/lib", "-lsecp256k1")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":credentials"))
                implementation(project(":identity"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val androidTest by getting
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                // Polyfill dependencies
                implementation(npm("stream-browserify", "3.0.0"))
                implementation(npm("buffer", "6.0.3"))
            }
        }
        val jsTest by getting
        val iosMain by getting
        val iosTest by getting

        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            languageSettings.useExperimentalAnnotation("kotlin.js.ExperimentalJsExport")
        }
    }

    npmPublishing {
        organization = "input-output-hk"
        access = RESTRICTED

        repositories {
            repository("github") {
                access = RESTRICTED
                registry = uri("https://npm.pkg.github.com")
                authToken = System.getenv("GITHUB_TOKEN")
            }
        }
        publications {
            val js by getting {
                moduleName = "prism-sdk"
                packageJson {
                    repository =
                        mutableMapOf(
                            Pair("type", "git"),
                            Pair("url", "https://github.com/input-output-hk/atala-tobearchived.git")
                        )
                }
            }
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

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }
}
