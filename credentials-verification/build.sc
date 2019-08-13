import mill._
import mill.scalalib._
import coursier.maven.MavenRepository

object app extends ScalaModule {
  def scalaVersion = "2.12.4"
  override def mainClass = Some("io.iohk.test.IssueCredential")

  override def ivyDeps = Agg(
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.beachape::enumeratum:1.5.13",
    ivy"com.lihaoyi::os-lib:0.2.7"
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.5"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object `indy-poc` extends ScalaModule {
  def scalaVersion = "2.12.4"

  override def mainClass = Some("io.iohk.indy.ExampleRunner")

  override def repositories() = super.repositories ++ Seq(
    MavenRepository("https://repo.sovrin.org/repository/maven-public")
  )

  override def ivyDeps = Agg(
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.lihaoyi::os-lib:0.2.7",
    ivy"org.hyperledger:indy:1.8.1-dev-985",
    ivy"org.slf4j:slf4j-api:1.7.26",
    // log4j is used by the indysdk java wrapper, using them is the simplest way to get the logs
    ivy"org.slf4j:slf4j-log4j12:1.8.0-alpha2",
    ivy"log4j:log4j:1.2.17"
  )
}
