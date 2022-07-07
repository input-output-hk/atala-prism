package io.iohk.atala.prism.node.operations

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Inside._

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

    "failparsing with an invalid curve point" in {
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
  }

}
