addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.2.1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.22")
addSbtPlugin("io.kamon" % "sbt-kanela-runner" % "2.0.13")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")
// GitHub Packages plugin (token optional; build.sbt imports sbtghpackages autoImport)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
// Yes, weirdly, it is mandated by scalapb documentation to put the following here
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.10"
