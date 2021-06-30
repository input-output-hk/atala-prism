import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
val pbandkVersion: String by rootProject.extra

repositories {
    mavenCentral()
    mavenLocal()
    // TODO: Replace with the mainstream version once https://github.com/streem/pbandk/issues/88
    //       is resolved.
    maven("https://vlad107.jfrog.io/artifactory/default-maven-virtual/")
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
        moduleName = "protos"
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
            kotlin.srcDir("${project(":protosLib").buildDir}/generated/source/proto/main/kotlin")
            dependencies {
                api("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
                api("com.benasher44:uuid:0.3.0")
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
                api("io.grpc:grpc-kotlin-stub:1.0.0")
                api("io.grpc:grpc-okhttp:1.36.0")
            }
        }
        val androidTest by getting {
            kotlin.srcDir("src/commonJvmAndroidTest/kotlin")
        }
        val jvmMain by getting {
            kotlin.srcDir("src/commonJvmAndroidMain/kotlin")
            dependencies {
                api("io.grpc:grpc-kotlin-stub:1.0.0")
                api("io.grpc:grpc-okhttp:1.36.0")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/commonJvmAndroidTest/kotlin")
        }
        val jsMain by getting {
            kotlin.srcDir("${project(":protosLib").buildDir}/generated/source/proto/jsMain/kotlin")
            dependencies {
                implementation(npm("grpc-web", "1.2.1", generateExternals = true))

                // Polyfill dependencies
                implementation(npm("stream-browserify", "3.0.0"))
                implementation(npm("buffer", "6.0.3"))
            }
        }
        val jsTest by getting
        val iosMain by getting
        val iosTest by getting

        all {
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

tasks {
    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }

    project(":protosLib").tasks
        .matching { it.name == "generateProto" }
        .all {
            val compileTasks = listOf<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>(
                named<KotlinCompile>("compileReleaseKotlinAndroid").get(),
                named<KotlinCompile>("compileDebugKotlinAndroid").get(),
                named<KotlinCompile>("compileKotlinJvm").get(),
                named<KotlinJsCompile>("compileKotlinJs").get(),
                named<KotlinNativeCompile>("compileKotlinIosX64").get(),
                named<KotlinNativeCompile>("compileKotlinIosArm64").get(),
                named<KotlinCompileCommon>("compileKotlinMetadata").get()
            )

            compileTasks.forEach {
                it.dependsOn(this)
                it.kotlinOptions {
                    freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
                }
            }
        }
}
