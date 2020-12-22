import com.google.protobuf.gradle.*

plugins {
    `java-library`
    id("com.google.protobuf") version "0.8.14"
}
val protobufVersion = "3.11.1"
val pbandkVersion: String by rootProject.extra

repositories {
    mavenCentral()
    mavenLocal()
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
                id("kotlin")
            }
        }
    }
}

tasks {
    compileJava {
        enabled = false
    }
}
