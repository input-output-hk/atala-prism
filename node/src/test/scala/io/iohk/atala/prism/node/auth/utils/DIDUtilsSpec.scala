package io.iohk.atala.prism.node.auth.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.auth.errors.UnknownPublicKeyId
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{CompressedECKeyData, DIDData, ECKeyData}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.ExecutionContext.Implicits.global

class DIDUtilsSpec extends AnyWordSpec with Matchers {
  val masterKeys: SecpPair = CryptoTestUtils.generateKeyPair()
  val masterEcKeyData: ECKeyData = protoECKeyDataFromPublicKey(
    masterKeys.publicKey
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

  def protoECKeyDataFromPublicKey(key: SecpPublicKey): ECKeyData = {
    node_models.ECKeyData(
      curve = key.curveName,
      x = ByteString.copyFrom(key.x),
      y = ByteString.copyFrom(key.y)
    )
  }

  def protoCompressedECKeyDataFromPublicKey(
      key: SecpPublicKey
  ): CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = key.curveName,
      data = ByteString.copyFrom(key.compressed)
    )

  "findPublicKey" should {
    "return key given publicKey contains compressedKeyData" in {
      val didData =
        DIDData(publicKeys = Seq(masterCompressedEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue.map(_.compressed.toVector) mustBe Right(
        masterKeys.publicKey.compressed.toVector
      )
    }

    "return key given publicKey contains not compressed dKeyData" in {
      val didData = DIDData(publicKeys = Seq(masterEcKeyDataPublicKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue.map(_.compressed.toVector) mustBe Right(
        masterKeys.publicKey.compressed.toVector
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
              curve = masterCompressedEcKeyData.curve,
              x = masterCompressedEcKeyData.data
            )
          )
        )
      val didData = DIDData(publicKeys = Seq(compressedAsUncompressedKey))
      DIDUtils.findPublicKey(didData, "master").value.futureValue.map(_.compressed.toVector) mustBe Right(
        masterKeys.publicKey.compressed.toVector
      )
    }
  }

}
