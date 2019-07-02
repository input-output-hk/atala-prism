import mill._
import mill.scalalib._

object app extends ScalaModule {
  def scalaVersion = "2.12.4"
  override def mainClass = Some("io.iohk.test.IssueCredential")

  override def ivyDeps = Agg(
    ivy"org.bouncycastle:bcprov-jdk15on:1.62",
    ivy"org.bouncycastle:bcpkix-jdk15on:1.62",
    ivy"com.typesafe.play::play-json:2.7.3",
    ivy"com.lihaoyi::os-lib:0.2.7"
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.5"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}
