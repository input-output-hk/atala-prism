plugins {
    id("com.eden.orchidPlugin") version "0.21.1"
    id("ank-gradle-plugin")
    kotlin("jvm")
}

val pbandkVersion: String by rootProject.extra

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
}

dependencies {
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidAll:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidKotlindoc:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidPluginDocs:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidWiki:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidSyntaxHighlighter:0.21.1")

    api(project(":crypto"))
    api(project(":protos"))
    api(project(":identity"))
    api(project(":credentials"))
}

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx/")
}

orchid {
    theme = "Editorial"
    version = project.version.toString()
    baseUrl = "http://docs-develop.atalaprism.io/"
}

ank {
    source = file("src/docs")
    target = file("src/orchid/resources/wiki/")
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks {
    project(":docs").tasks
        .matching { it.name == "runAnk" }
        .all {
            val orchidBuildTask = named("orchidBuild").get()
            val orchidServeTask = named("orchidServe").get()

            listOf(orchidBuildTask, orchidServeTask).forEach {
                it.dependsOn(this)
            }
        }
}
