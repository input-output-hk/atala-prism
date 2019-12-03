package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.wallet.protos.{ECPublicKey, KeyPair}
import io.iohk.nodenew.{geud_node_new => proto}
object ECKeyOperation {

  /**
    *
    * @param keyPairs
    *  key pair with id master is required for signing the document
    * @return
    */
  def toSignedAtalaOperation(keyPairs: Seq[KeyPair]): proto.SignedAtalaOperation = {

    val publicKeys = keyPairs.map { keyPair =>
      toPublicKey(
        keyPair.id,
        toECKeyData(keyPair.publicKey.getOrElse(throw new RuntimeException(s"Invalid public key ${keyPair.id}"))),
        keyPair.usage
      )
    }

    val masterKeyPrivate = keyPairs.find(_.id == "master").getOrElse(throw new RuntimeException("Missing master key"))

    val didData = proto.DIDData(publicKeys = publicKeys)
    val createDIDOperation = proto.CreateDIDOperation(Some(didData))
    val atalaOperation = proto.AtalaOperation(proto.AtalaOperation.Operation.CreateDid(createDIDOperation))
    val signature = ECSignature.sign(
      toPrivateKey(masterKeyPrivate.privateKey.getOrElse(throw new RuntimeException("Missing private key"))),
      atalaOperation.toByteArray
    )
    proto.SignedAtalaOperation(
      signedWith = "master",
      signature = ByteString.copyFrom(signature.toArray),
      operation = Some(atalaOperation)
    )

  }

  def toPublicKey(proto: protos.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(throw new RuntimeException("Invalid public key"))
  }

  private def toPrivateKey(proto: protos.ECPrivateKey): PrivateKey = {
    val maybe = for {
      d <- proto.d.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPrivateKey(d)

    maybe.getOrElse(throw new RuntimeException("Invalid public key"))
  }

  private def toECKeyData(key: ECPublicKey): proto.ECKeyData = {
    proto
      .ECKeyData()
      .withCurve(ECKeys.CURVE_NAME)
      .withX(ByteString.copyFrom(key.x.getOrElse(throw new RuntimeException("Invalid point X ")).toByteArray))
      .withY(ByteString.copyFrom(key.y.getOrElse(throw new RuntimeException("Invalid point Y ")).toByteArray))
  }

  private def toPublicKey(id: String, ecKeyData: proto.ECKeyData, keyUsage: proto.KeyUsage): proto.PublicKey = {
    proto
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
  }

}
