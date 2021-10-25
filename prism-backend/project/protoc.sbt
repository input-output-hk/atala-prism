addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
// Yes, weirdly, it is mandated by scalapb documentation to put the following here
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.6"
