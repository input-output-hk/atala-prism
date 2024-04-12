package io.iohk.atala.prism.node.auth.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.auth.errors.UnknownPublicKeyId
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{CompressedECKeyData, DIDData, ECKeyData}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.ExecutionContext.Implicits.global

class DIDUtilsSpec extends AnyWordSpec with Matchers {
  val masterKeys: ECKeyPair = EC.generateKeyPair()
  val masterEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(
    masterKeys.getPublicKey
  )
  val masterEcKeyDataPublicKey = node_models
    .PublicKey(
      "master",
      node_models.KeyUsage.MASTER_KEY,
      Some(
        node_models.LedgerData()
      ),
      None,
      node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
    )
  val masterCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(masterKeys.getPublicKey)
  val masterCompressedEcKeyDataPublicKey = node_models
    .PublicKey(
      "master",
      node_models.KeyUsage.MASTER_KEY,
      Some(
        node_models.LedgerData()
      ),
      None,
      node_models.PublicKey.KeyData.CompressedEcKeyData(
        masterCompressedEcKeyData
      )
    )

  def protoECKeyDataFromPublicKey(key: ECPublicKey): ECKeyData = {
    val point = key.getCurvePoint

    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

  def protoCompressedECKeyDataFromPublicKey(
      key: ECPublicKey
  ): CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = ECConfig.getCURVE_NAME,
      data = ByteString.copyFrom(key.getEncodedCompressed)
    )

  "findPublicKey" should {
    "return key given publicKey contains compressedKeyData" in {
      val didData =
        DIDData(publicKeys = Seq(masterCompressedEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue mustBe Right(
        masterKeys.getPublicKey
      )
    }

    "return key given publicKey contains not compressed dKeyData" in {
      val didData = DIDData(publicKeys = Seq(masterEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue mustBe Right(
        masterKeys.getPublicKey
      )
    }

    "return AuthError given key not found" in {
      val didData = DIDData(publicKeys = Seq(masterEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "unknown").value.futureValue mustBe Left(
        UnknownPublicKeyId()
      )
    }

    "work fine when you pass compressed key as uncompressed" in {
      val compressedAsUncompressedKey =
        masterCompressedEcKeyDataPublicKey.copy(keyData =
          node_models.PublicKey.KeyData.EcKeyData(
            node_models.ECKeyData(
              curve = ECConfig.getCURVE_NAME,
              x = masterCompressedEcKeyData.data
            )
          )
        )
      val didData = DIDData(publicKeys = Seq(compressedAsUncompressedKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue mustBe Right(
        masterKeys.getPublicKey
      )
    }
  }

}
