addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")
// Yes, weirdly, it is mandated by scalapb documentation to put the following here
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
libraryDependencies += "com.thesamet.scalapb.grpcweb" %% "scalapb-grpcweb-code-gen" % "0.4.1"
