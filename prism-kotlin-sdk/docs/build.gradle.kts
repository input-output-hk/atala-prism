plugins {
    id("com.eden.orchidPlugin") version "0.21.1"
}

dependencies {
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidAll:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidKotlindoc:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidPluginDocs:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidWiki:0.21.1")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidSyntaxHighlighter:0.21.1")
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
