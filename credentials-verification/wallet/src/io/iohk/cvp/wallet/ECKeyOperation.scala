package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.prism.protos.{node_models, wallet_internal, wallet_models}

object ECKeyOperation {

  /**
    *
    * @param keyPairs
    *  key pair with id master is required for signing the document
    * @return
    */
  def toSignedAtalaOperation(keyPairs: Seq[wallet_internal.KeyPair]): node_models.SignedAtalaOperation = {

    val publicKeys = keyPairs.map { keyPair =>
      toPublicKey(
        keyPair.id,
        toECKeyData(keyPair.publicKey.getOrElse(throw new RuntimeException(s"Invalid public key ${keyPair.id}"))),
        keyPair.usage
      )
    }

    val masterKeyPrivate = keyPairs.find(_.id == "master").getOrElse(throw new RuntimeException("Missing master key"))

    val didData = node_models.DIDData(publicKeys = publicKeys)
    val createDIDOperation = node_models.CreateDIDOperation(Some(didData))
    val atalaOperation = node_models.AtalaOperation(node_models.AtalaOperation.Operation.CreateDid(createDIDOperation))
    val signature = ECSignature.sign(
      toPrivateKey(masterKeyPrivate.privateKey.getOrElse(throw new RuntimeException("Missing private key"))),
      atalaOperation.toByteArray
    )
    node_models.SignedAtalaOperation(
      signedWith = "master",
      signature = ByteString.copyFrom(signature.toArray),
      operation = Some(atalaOperation)
    )
  }

  def toPublicKey(proto: wallet_models.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(throw new RuntimeException("Invalid public key"))
  }

  private def toPrivateKey(proto: wallet_models.ECPrivateKey): PrivateKey = {
    val maybe = for {
      d <- proto.d.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPrivateKey(d)

    maybe.getOrElse(throw new RuntimeException("Invalid public key"))
  }

  private def toECKeyData(key: wallet_models.ECPublicKey): node_models.ECKeyData = {
    val x = key.x.getOrElse(throw new RuntimeException("Invalid point X")).value
    val y = key.y.getOrElse(throw new RuntimeException("Invalid point Y")).value
    node_models
      .ECKeyData()
      .withCurve(ECKeys.CURVE_NAME)
      .withX(ByteString.copyFrom(toUnsignedByteArray(BigInt(x))))
      .withY(ByteString.copyFrom(toUnsignedByteArray(BigInt(y))))
  }

  private def toPublicKey(
      id: String,
      ecKeyData: node_models.ECKeyData,
      keyUsage: node_models.KeyUsage
  ): node_models.PublicKey = {
    node_models
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
  }

  private def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    src.toByteArray.dropWhile(_ == 0)
  }
}
