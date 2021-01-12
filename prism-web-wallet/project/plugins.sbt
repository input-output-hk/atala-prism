resolvers += Resolver.bintrayRepo("oyvindberg", "converter")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// Required before "sbt-scalajs"
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.0.0"

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta29.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.3.0")

addSbtPlugin("com.alexitc" % "sbt-chrome-plugin" % "0.7.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
