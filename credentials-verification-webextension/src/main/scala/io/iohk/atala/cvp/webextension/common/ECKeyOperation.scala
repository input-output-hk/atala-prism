package io.iohk.atala.cvp.webextension.common

import com.google.protobuf.ByteString
import io.iohk.atala.cvp.webextension.facades.bn.ReducedBigNumber
import io.iohk.prism.protos.node_models._
import typings.bip32.bip32Mod.BIP32Interface
import typings.bip32.{mod => bip32}
import typings.elliptic.mod.ec
import typings.elliptic.mod.ec.{KeyPair, Signature}
import typings.hashJs.{hashJsStrings, mod => hash}

import scala.scalajs.js
import scala.scalajs.js.typedarray._

object ECKeyOperation {
  val CURVE_NAME = "secp256k1"
  val firstMasterKeyId = "master"

  // https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
  private val firstMasterChild = "m/0'/0'/0'"

  def didFromMasterKey(ecKeyPair: EcKeyPair): String = {
    val publicKey = toPublicKey(firstMasterKeyId, toECKeyData(ecKeyPair.publicKeyPair), KeyUsage.MASTER_KEY)
    val atalaOperation = createAtalaOperation(Seq(publicKey))
    val byteArray = atalaOperation.toByteArray.toTypedArray
    val uint8Array = new Uint8Array(byteArray.buffer, byteArray.byteOffset, byteArray.length)
    val sha256 = hash.sha256().update(uint8Array)
    val didSuffix = sha256.digest_hex(hashJsStrings.hex)
    val did = s"did:prism:$didSuffix"
    did
  }

  def createDIDOperation(ecKeyPair: EcKeyPair): AtalaOperation = {
    val publicKey = toPublicKey(firstMasterKeyId, toECKeyData(ecKeyPair.publicKeyPair), KeyUsage.MASTER_KEY)
    createAtalaOperation(Seq(publicKey))
  }

  def createECKeyPair(mnemonic: Mnemonic): EcKeyPair = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    EcKeyPair(root.derivePath(firstMasterChild))
  }

  private def createAtalaOperation(publicKeys: Seq[PublicKey]): AtalaOperation = {
    val didData = DIDData(publicKeys = publicKeys)
    val createDIDOperation = CreateDIDOperation(Some(didData))
    val atalaOperation = AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOperation))
    atalaOperation
  }

  def toSignedAtalaOperation(mnemonic: Mnemonic): SignedAtalaOperation = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    val ecKeyPair = EcKeyPair(root.derivePath(firstMasterChild))
    val publicKey =
      toPublicKey(firstMasterKeyId, toECKeyData(ecKeyPair.publicKeyPair), KeyUsage.MASTER_KEY)
    val atalaOperation = createAtalaOperation(Seq(publicKey))
    val byteArray = atalaOperation.toByteArray.toTypedArray
    val uint8Array = new Uint8Array(byteArray.buffer, byteArray.byteOffset, byteArray.length)
    val sha256 = hash.sha256().update(uint8Array)
    val hashed = sha256.digest_hex(hashJsStrings.hex)
    val signature: Signature =
      ecKeyPair.privateKeyPair.sign(hashed)

    SignedAtalaOperation(
      signedWith = firstMasterKeyId,
      signature = ByteString.copyFrom(signature.toDER().asInstanceOf[Int8Array].toArray),
      operation = Some(atalaOperation)
    )
  }

  private def toECKeyData(key: KeyPair): ECKeyData = {
    val publicKey = key.getPublic()
    val x = coordinateToByteArray(publicKey.getX)
    val y = coordinateToByteArray(publicKey.getY)

    ECKeyData()
      .withCurve(CURVE_NAME)
      .withX(ByteString.copyFrom(x))
      .withY(ByteString.copyFrom(y))
  }

  private def coordinateToByteArray(coordinate: js.Any): Array[Byte] = {
    val hex = coordinate.asInstanceOf[ReducedBigNumber].toString("hex")
    hex
      .grouped(2)
      .map { h =>
        Integer.parseInt(h, 16).toByte
      }
      .toArray
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
