package io.iohk.atala.prism.node.operations

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Inside._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.models.ProtocolConstants

import javax.xml.bind.DatatypeConverter
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.CompressedECKeyData

class ParsingUtilsSpec extends AnyWordSpec with Matchers {
  "ParsingUtils" should {
    "missing values" in {
      val xByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary(""))
      val yByteString: ByteString = ByteString.copyFrom(DatatypeConverter.parseHexBinary(""))
      inside(
        ParsingUtils.parseECKey(
          ValueAtPath(
            new node_models.ECKeyData(
              /* curve =*/ ProtocolConstants.secpCurveName,
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
              /* curve =*/ ProtocolConstants.secpCurveName,
              /* x = */ xByteString,
              /* y = */ yByteString
            ),
            Path(Vector.empty)
          )
        )
      ) { case Left(ValidationError.InvalidValue(path, value, explanation)) =>
        path.path mustBe Vector()
        value mustBe ""
        explanation mustBe "Unable to initialize the key: invalid KeySpec: Point not on curve"
      // explanation mustBe "Unable to initialize the key: invalid KeySpec: Point not on curve" // Error before ATL-974
      }
    }

    "fail parseCompressedECKey when curve in provided argument does mot match curve in EC config" in {

      val keyPair = CryptoTestUtils.generateKeyPair()
      val pk = keyPair.publicKey
      val validData = ByteString.copyFrom(pk.compressed)

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
        case Left(err) => err.explanation mustBe "Unsupported curve - InvalidCurve"
        case Right(_) => fail("parseCompressedECKey did not fail with invalid curve provided")
      }

    }

    "parse key successfully with valid key curves" in {

      val keyPair = CryptoTestUtils.generateKeyPair()

      val dataByteString: ByteString = ByteString.copyFrom(keyPair.publicKey.compressed)

      val pks = ProtocolConstants.supportedEllipticCurves.map { curve =>
        new CompressedECKeyData(
          curve,
          dataByteString
        )
      }

      pks.foreach { pk =>
        val parsed = ParsingUtils.parseCompressedECKey(
          ValueAtPath(
            pk,
            Path(Vector.empty)
          )
        )
        inside(parsed) {
          case Left(value) =>
            fail(value.explanation)
          case Right(value) =>
            pk.curve mustBe value.curveName
            pk.data.toByteArray.sameElements(dataByteString.toByteArray) mustBe true
        }
      }
    }
  }

}
