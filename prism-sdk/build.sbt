// Set Scala version for aggregate commands (e.g., test coverage)
scalaVersion := SdkBuild.scala213

lazy val sdk = SdkBuild.sdk

lazy val prismCrypto = SdkBuild.prismCrypto
lazy val prismProtos = SdkBuild.prismProtos
lazy val prismIdentity = SdkBuild.prismIdentity
lazy val prismCredentials = SdkBuild.prismCredentials
lazy val prismConnector = SdkBuild.prismConnector
lazy val mirror = SdkBuild.mirror
lazy val prismDocs = SdkBuild.prismDocs
