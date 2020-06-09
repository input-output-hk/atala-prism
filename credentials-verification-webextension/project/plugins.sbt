resolvers += Resolver.bintrayRepo("oyvindberg", "converter")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// Required before "sbt-scalajs"
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.0.0"

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta12")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1")

addSbtPlugin("com.alexitc" % "sbt-chrome-plugin" % "0.7.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.17.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.29")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.4")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.2"

libraryDependencies += "com.thesamet.scalapb.grpcweb" %% "scalapb-grpcweb-code-gen" % "0.2.0+21-b26c9e0a-SNAPSHOT"
