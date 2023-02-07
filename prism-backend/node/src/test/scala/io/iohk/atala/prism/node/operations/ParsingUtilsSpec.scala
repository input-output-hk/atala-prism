package io.iohk.atala.prism.node.operations

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Inside._
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import com.google.protobuf.ByteString
import javax.xml.bind.DatatypeConverter

import io.iohk.atala.prism.node.operations.path.{ValueAtPath, Path}
import io.iohk.atala.prism.protos.node_models

class ParsingUtilsSpec extends AnyWordSpec with Matchers {
  "ParsingUtils" should {
    "missing values" in {
      val xByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary(""))
      val yByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary(""))
      inside(
        ParsingUtils.parseECKey(
          ValueAtPath(
            new node_models.ECKeyData(
              /* curve =*/ io.iohk.atala.prism.crypto.ECConfig.INSTANCE.getCURVE_NAME,
              /* x = */ xByteString,
              /* y = */ yByteString
            ),
            Path(Vector.empty)
          )
        )
      ) { case Left(ValidationError.MissingValue(path)) =>
        path.path mustBe Vector("x")
      }
    }

    "fail parsing with an invalid curve point" in {
      val xByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary("01"))
      val yByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary("02"))
      inside(
        ParsingUtils.parseECKey(
          ValueAtPath(
            new node_models.ECKeyData(
              /* curve =*/ io.iohk.atala.prism.crypto.ECConfig.INSTANCE.getCURVE_NAME,
              /* x = */ xByteString,
              /* y = */ yByteString
            ),
            Path(Vector.empty)
          )
        )
      ) { case Left(ValidationError.InvalidValue(path, value, explanation)) =>
        path.path mustBe Vector()
        value mustBe ""
        explanation mustBe "Unable to initialize the key: ECPoint corresponding to a public key doesn't belong to Secp256k1 curve"
      // explanation mustBe "Unable to initialize the key: invalid KeySpec: Point not on curve" // Error before ATL-974
      }
    }

    "fail parseCompressedECKey when curve in provided argument does mot match curve in EC config" in {

      val keyPair = EC.generateKeyPair()
      val pk = keyPair.getPublicKey
      val validData = ByteString.copyFrom(pk.getEncodedCompressed)

      val invalidCompressedKey = node_models.CompressedECKeyData(
        curve = "InvalidCurve", // some random string that is not ECConfig.getCURVE_NAME
        data = validData
      )

      inside(
        ParsingUtils.parseCompressedECKey(
          ValueAtPath(
            invalidCompressedKey,
            Path(Vector.empty)
          )
        )
      ) {
        case Left(err) => err.explanation mustBe "Unsupported curve"
        case Right(_) => fail("parseCompressedECKey did not fail with invalid curve provided")
      }

    }
  }

}
