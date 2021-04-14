plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("com.android.library")
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
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.robolectric:android-all:10-robolectric-5803371")
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
            }
        }
        val jsMain by getting
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
