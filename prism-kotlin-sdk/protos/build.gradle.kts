import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
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
            task.dependsOn(":generator:jar")
            task.builtins {
                remove("java")
            }
            task.plugins {
                id("kotlin") {
                    option("kotlin_package=io.iohk.atala.prism.kotlin.protos")
                    option("kotlin_service_gen=${project(":generator").buildDir}/libs/generator-$version.jar|io.iohk.atala.prism.kotlin.generator.Generator")
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
            kotlin.srcDir("$buildDir/generated/source/proto/main/kotlin")
            dependencies {
                api("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
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
                api("io.grpc:grpc-kotlin-stub:1.0.0")
                api("io.grpc:grpc-okhttp:1.36.0")
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
                implementation(npm("grpc-web", "1.2.1", generateExternals = true))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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

dependencies {
    // This is needed for well-known protobuf types (such as `google/protobuf`) to be discoverable
    implementation("pro.streem.pbandk:pbandk-runtime-jvm:$pbandkVersion")
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
