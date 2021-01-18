import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    kotlin("multiplatform")
    `maven-publish`
    `java-library`
    id("com.google.protobuf")
}
val protobufVersion = "3.11.1"
val pbandkVersion: String by rootProject.extra

repositories {
    mavenCentral()
    mavenLocal()
    // TODO: Replace with the mainstream version once https://github.com/streem/pbandk/issues/88
    //       is resolved.
    maven { setUrl("https://dl.bintray.com/itegulov/maven") }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("kotlin") {
            artifact = "pro.streem.pbandk:protoc-gen-kotlin-jvm:$pbandkVersion:jvm8@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.builtins {
                remove("java")
            }
            task.plugins {
                id("kotlin") {
                    option("kotlin_package=io.iohk.atala.prism.kotlin.protos")
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {
            setSrcDirs(listOf("src/main/proto"))
            setIncludes(
                listOf(
                    "common_*.proto",
                    "node_*.proto"
                )
            )
        }
    }
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
            kotlin.srcDir("$buildDir/generated/source/proto/main/kotlin")
            dependencies {
                api("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting
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
    compileJava {
        enabled = false
    }

    "jvmTest"(Test::class) {
        useJUnitPlatform()
    }

    project(":protos").tasks
        .matching { it.name == "generateProto" }
        .all {
            val compileTasks = listOf<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>(
                named<KotlinCompile>("compileKotlinJvm").get(),
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
