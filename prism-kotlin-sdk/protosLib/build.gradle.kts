import com.google.protobuf.gradle.*

plugins {
    `java-library`
    id("com.google.protobuf")
}

val pbandkVersion: String by rootProject.extra
val protobufVersion = "3.11.1"

dependencies {
    // This is needed for includes, ref: https://github.com/google/protobuf-gradle-plugin/issues/41#issuecomment-143884188
    compileOnly("com.google.protobuf:protobuf-java:$protobufVersion")
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

tasks {
    compileJava {
        enabled = false
    }
}
