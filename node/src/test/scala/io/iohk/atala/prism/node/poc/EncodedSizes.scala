package io.iohk.atala.prism.node.poc

import java.util.Base64
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.protos.node_models

object EncodedSizes {
  def main(args: Array[String]): Unit = {
    val startTime = System.currentTimeMillis()

    val n = 100000
    println(s"Generating $n dids")

    val data = for {
      _ <- 1 to n
      masterPublicKey1 = EC.generateKeyPair().getPublicKey
      masterPublicKey2 = EC.generateKeyPair().getPublicKey
      masterPublicKey3 = EC.generateKeyPair().getPublicKey
      did = createDID(
        List(masterPublicKey1, masterPublicKey2, masterPublicKey3)
      )
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

  def createDID(masterPublicKeys: List[ECPublicKey]): String = {
    def keyElement(publicKey: ECPublicKey, index: Int): node_models.PublicKey =
      node_models.PublicKey(
        id = s"master$index",
        usage = node_models.KeyUsage.MASTER_KEY,
        keyData = node_models.PublicKey.KeyData.EcKeyData(
          publicKeyToProto(publicKey)
        )
      )

    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.CreateDIDOperation.DIDCreationData(
          publicKeys = masterPublicKeys.zipWithIndex map { case (k, i) =>
            keyElement(k, i)
          }
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = Sha256.compute(operationBytes)
    val didSuffix: DidSuffix = DidSuffix.fromDigest(operationHash)
    val encodedOperation = Base64.getUrlEncoder.encodeToString(operationBytes)
    s"did:prism:${didSuffix.getValue}:$encodedOperation"
  }

  private def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }
}
