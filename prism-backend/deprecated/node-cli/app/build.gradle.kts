java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.ADOPTOPENJDK)
    }
}

plugins {
    java

    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.0"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
    google()
    maven("https://plugins.gradle.org/m2/")
    maven {
        url = uri("https://maven.pkg.github.com/input-output-hk/atala-prism-sdk")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("ATALA_GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("ATALA_GITHUB_TOKEN")
        }
    }
    // Needed for Kotlin coroutines that support new memory management mode
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}

val prismSdkVersion = "1.3.0-build-33-bfbdc87c"

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:30.1.1-jre")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // needed for cryptography primitives implementation
    implementation("io.iohk.atala:prism-crypto:$prismSdkVersion")

    // needed to deal with DIDs
    implementation("io.iohk.atala:prism-identity:$prismSdkVersion")

    // needed to deal with credentials
    implementation("io.iohk.atala:prism-credentials:$prismSdkVersion")

    // needed to interact with PRISM Node service
    implementation("io.iohk.atala:prism-api:$prismSdkVersion")

    // needed for the credential content, bring the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    // needed for dealing with dates, bring the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")

    // Fixes a bug from SLF4J
    implementation("org.slf4j:slf4j-simple:1.7.32")

    // Fixes a build issue
    implementation("com.soywiz.korlibs.krypto:krypto-jvm:2.0.6")
}

application {
    // Define the main class for the application.
    mainClass.set("io.iohk.atala.prism.CliKt")
}

val fatJar = task("fatJar", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Title"] = "Node CLI JAR"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "io.iohk.atala.prism.CliKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}
