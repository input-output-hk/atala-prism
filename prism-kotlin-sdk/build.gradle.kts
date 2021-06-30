import com.palantir.gradle.gitversion.VersionDetails
import java.net.URI

plugins {
    kotlin("multiplatform") version "1.5.10" apply false
    kotlin("plugin.serialization") version "1.5.10" apply false
    kotlin("native.cocoapods") version "1.5.10" apply false
    id("com.android.library") version "4.1.2" apply false
    id("com.google.protobuf") version "0.8.14" apply false
    id("dev.petuska.npm.publish") version "2.0.2" apply false
    id("com.palantir.git-version") version "0.12.3"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    `maven-publish`
}
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val pbandkVersion by extra("0.10.0-M4")

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
        google()
        maven("https://plugins.gradle.org/m2/")
        maven("https://vlad107.jfrog.io/artifactory/default-maven-virtual/")
    }

    if (listOf("protos", "crypto", "identity", "credentials", "extras").contains(name)) {
        apply(plugin = "org.gradle.maven-publish")

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = URI("https://maven.pkg.github.com/input-output-hk/atala-tobearchived")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
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
