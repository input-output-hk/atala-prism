import com.palantir.gradle.gitversion.VersionDetails

plugins {
    kotlin("multiplatform") version "1.4.30" apply false
    kotlin("plugin.serialization") version "1.4.30" apply false
    kotlin("native.cocoapods") version "1.4.30" apply false
    id("com.google.protobuf") version "0.8.14" apply false
    id("com.palantir.git-version") version "0.12.3"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val pbandkVersion by extra("0.10.0-M2")

// Since NPM only accepts pure semantic versioning (\d.\d.\d), we have to
// replace 0.1.0-$githash with 0.1.0 in root package.json
gradle.buildFinished {
    val rootPackageJson = File("$buildDir/js/package.json")
    val versionRegex = Regex("""  "version": "(\d\.\d\.\d)-[0-9a-f]{8}"""")
    if (rootPackageJson.exists()) {
        val newLines = rootPackageJson.readLines().map { line ->
            val matchResult = versionRegex.matchEntire(line)
            if (matchResult != null) {
                "  \"version\": \"${matchResult.groups[1]!!.value}\""
            } else {
                line
            }
        }
        rootPackageJson.writeText(newLines.joinToString("\n"))
    }
}

allprojects {
    group = "io.iohk.atala.prism"
    version = "0.1.0-" + versionDetails().gitHash.substring(0, 8)

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://plugins.gradle.org/m2/")
        maven { setUrl("https://dl.bintray.com/itegulov/maven") }
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        verbose.set(true)
        outputToConsole.set(true)

        // Exclude generated proto classes
        filter {
            exclude { element ->
                element.file.path.contains("generated/") or
                    element.file.path.contains("externals/")
            }
        }
    }
}
