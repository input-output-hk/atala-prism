resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")

addSbtPlugin("org.scalablytyped" % "sbt-scalablytyped" % "202001240947")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.29")
addSbtPlugin(
  "net.lullabyte" % "sbt-chrome-plugin" % "1b6d0d9cbeb95a23d5ecd4ba0defd6f1373fae1b"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler-sjs06" % "0.16.0")
