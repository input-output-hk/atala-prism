package io.iohk.atala.cvp.webextension.common

import com.google.protobuf.ByteString
import io.iohk.prism.protos.node_models._
import typings.bip32.bip32Mod.BIP32Interface
import typings.bip32.{mod => bip32}
import typings.bnJs.{mod => bn}
import typings.elliptic.mod.ec
import typings.elliptic.mod.ec.{KeyPair, Signature}

import scala.collection.mutable
import scala.scalajs.js.typedarray._

object ECKeyOperation {
  val CURVE_NAME = "secp256k1"

  def toSignedAtalaOperation(mnemonic: Mnemonic): SignedAtalaOperation = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    val ecKeyPair = EcKeyPair(root)
    val publicKey =
      toPublicKey("master", toECKeyData(ecKeyPair.publicKeyPair), KeyUsage.MASTER_KEY)

    val didData = DIDData(publicKeys = Seq(publicKey))

    val createDIDOperation = CreateDIDOperation(Some(didData))
    val atalaOperation = AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOperation))
    val signature: Signature = ecKeyPair.privateKeyPair.sign(atalaOperation.toByteArray.toTypedArray)
    SignedAtalaOperation(
      signedWith = "master",
      signature = ByteString.copyFrom(signature.toDER().asInstanceOf[Int8Array].toArray),
      operation = Some(atalaOperation)
    )
  }

  private def toECKeyData(key: KeyPair): ECKeyData = {
    val publicKey = key.getPublic()
    val x: mutable.Seq[Byte] = publicKey.getX.asInstanceOf[bn.BN].toArray().map(_.toByte)
    val y: mutable.Seq[Byte] = publicKey.getY.asInstanceOf[bn.BN].toArray().map(_.toByte)
    ECKeyData()
      .withCurve(CURVE_NAME)
      .withX(ByteString.copyFrom(x.toArray))
      .withY(ByteString.copyFrom(y.toArray))
  }

  private def toPublicKey(
      id: String,
      ecKeyData: ECKeyData,
      keyUsage: KeyUsage
  ) = {
    PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
  }

}

case class EcKeyPair(privateKeyPair: KeyPair, publicKeyPair: KeyPair)
object EcKeyPair {
  val CURVE_NAME = "secp256k1"
  val ec = new ec(CURVE_NAME)
  def apply(root: BIP32Interface): EcKeyPair = {
    new EcKeyPair(ec.keyFromPrivate(root.privateKey.get), ec.keyFromPublic(root.publicKey))
  }
}
