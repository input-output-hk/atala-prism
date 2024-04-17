package io.iohk.atala.prism.auth.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.errors.UnknownPublicKeyId
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{CompressedECKeyData, DIDData, ECKeyData}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.ExecutionContext.Implicits.global
import identus.apollo.PublicKey
import identus.apollo.MyKeyPair

class DIDUtilsSpec extends AnyWordSpec with Matchers {
  val masterKeys: MyKeyPair = MyKeyPair.generateKeyPair
  val masterEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(masterKeys.publicKey)
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
    protoCompressedECKeyDataFromPublicKey(masterKeys.publicKey)
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

  def protoECKeyDataFromPublicKey(key: PublicKey): ECKeyData = {
    val point = key.toCurvePoint
    node_models.ECKeyData(
      curve = key.curveName,
      x = ByteString.copyFrom(point.x),
      y = ByteString.copyFrom(point.y)
    )
  }

  def protoCompressedECKeyDataFromPublicKey(
      key: PublicKey
  ): CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = key.curveName,
      data = ByteString.copyFrom(key.getEncodedCompressed)
    )

  "findPublicKey" should {
    "return key given publicKey contains compressedKeyData" in {
      val didData =
        DIDData(publicKeys = Seq(masterCompressedEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue mustBe Right(
        masterKeys.publicKey
      )
    }

    "return key given publicKey contains not compressed dKeyData" in {
      val didData = DIDData(publicKeys = Seq(masterEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue mustBe Right(
        masterKeys.publicKey
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
        masterKeys.publicKey
      )
    }
  }

}
