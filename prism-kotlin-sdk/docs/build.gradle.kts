plugins {
    kotlin("jvm")
    id("ank-gradle-plugin")
    id("com.palantir.git-version")
}

repositories {
    mavenCentral()
    maven("https://vlad107.jfrog.io/artifactory/default-maven-virtual/")
}

dependencies {
    api(project(":crypto"))
    api(project(":protos"))
    api(project(":identity"))
    api(project(":credentials"))
    api(project(":extras"))
}

ank {
    source = file("src/docs")
    target = file("src/docusaurus/docs")
    classpath = sourceSets.main.get().runtimeClasspath
}
