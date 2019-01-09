package io.iohk.cef.crypto

import org.scalatest.FlatSpec
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pureconfig._
import com.typesafe.config.ConfigFactory
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.Matchers._
import org.scalatest.Inside._
import org.scalactic.Equivalence
import io.iohk.cef.crypto.test.utils.CryptoEntityArbitraries

class EntityConfigSpec extends FlatSpec with CryptoEntityArbitraries {

  behavior of "CryptoEntities Config"

  it should "work correctly with SigningPublicKey" in { testReadFromConfig[SigningPublicKey] }
  it should "work correctly with SigningPrivateKey" in { testReadFromConfig[SigningPrivateKey] }
  it should "work correctly with EncryptionPublicKey" in { testReadFromConfig[EncryptionPublicKey] }
  it should "work correctly with EncryptionPrivateKey" in { testReadFromConfig[EncryptionPrivateKey] }
  it should "work correctly with Hash" in { testReadFromConfig[Hash] }
  it should "work correctly with Signature" in { testReadFromConfig[Signature] }
  it should "work correctly with EncryptedData" in { testReadFromConfig[EncryptedData] }

  def testReadFromConfig[T: Arbitrary: ConfigReader: Equivalence]: Unit = {
    import pureconfig.generic.auto._

    case class Conf(t: T)

    forAll(arbitrary[T]) { t =>
      val hocon = s"""
        t = \"\"\"${t}\"\"\"
      """

      val config = ConfigFactory.parseString(hocon)
      inside(pureconfig.loadConfig[Conf](config)) {
        case Right(conf) =>
          conf.t shouldBe t
      }
    }

  }

}
