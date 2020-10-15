package io.iohk.atala.prism.node.poc

import java.security.PublicKey
import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{ECKeys, SHA256Digest}
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.protos.node_models

object EncodedSizes {
  def main(args: Array[String]): Unit = {
    val startTime = System.currentTimeMillis()

    val n = 100000
    println(s"Generating $n dids")

    val data = for {
      _ <- 1 to n
      masterPublicKey1 = ECKeys.generateKeyPair().getPublic
      masterPublicKey2 = ECKeys.generateKeyPair().getPublic
      masterPublicKey3 = ECKeys.generateKeyPair().getPublic
      did = createDID(List(masterPublicKey1, masterPublicKey2, masterPublicKey3))
    } yield (did, did.length)

    val sortedData = data.sortBy(_._2)
    println("printing 3 shortest DIDs")
    println(sortedData.take(3).mkString("\n"))
    println("printing 3 longest DIDs")
    println(sortedData.drop(n - 3).mkString("\n"))

    val averageSize = data.foldLeft(0) { _ + _._2 } / n.toDouble
    println(s"Average DID length $averageSize bytes")
    val endTime = System.currentTimeMillis()

    println(s"Dataset generated in ${(endTime - startTime) / 1000.0} seconds")

  }

  def createDID(masterPublicKeys: List[PublicKey]): String = {
    def keyElement(publicKey: PublicKey, index: Int): node_models.PublicKey =
      node_models.PublicKey(
        id = s"master$index",
        usage = node_models.KeyUsage.MASTER_KEY,
        keyData = node_models.PublicKey.KeyData.EcKeyData(
          publicKeyToProto(publicKey)
        )
      )

    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys =
            masterPublicKeys.zipWithIndex map { case (k, i) => keyElement(k, i) }
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = SHA256Digest.compute(operationBytes)
    val didSuffix = DIDSuffix(operationHash)
    val encodedOperation = Base64.getUrlEncoder.encodeToString(operationBytes)
    s"did:prism:${didSuffix.suffix}:$encodedOperation"
  }

  private def publicKeyToProto(key: PublicKey): node_models.ECKeyData = {
    val point = ECKeys.getECPoint(key)
    node_models.ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }
}
