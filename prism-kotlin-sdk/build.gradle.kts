import com.palantir.gradle.gitversion.VersionDetails

plugins {
    kotlin("multiplatform") version "1.4.21" apply false
    kotlin("native.cocoapods") version "1.4.21" apply false
    id("com.google.protobuf") version "0.8.14" apply false
    id("com.palantir.git-version") version "0.12.3"
}
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val pbandkVersion by extra("0.10.0-M1")

subprojects {
    group = "io.iohk.atala.prism"
    version = "0.1-" + versionDetails().gitHash.substring(0, 8)
    
    repositories {
        mavenCentral()
        mavenLocal()
        maven { setUrl("https://dl.bintray.com/itegulov/maven") }
    }
}
