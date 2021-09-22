addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")
// Yes, weirdly, it is mandated by scalapb documentation to put the following here
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.11"