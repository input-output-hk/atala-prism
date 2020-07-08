resolvers += Resolver.bintrayRepo("oyvindberg", "converter")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta12")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.1")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.18.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

// required to package a fat-jar to use on Android
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
