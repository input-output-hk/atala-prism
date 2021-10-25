package io.iohk.prism

import java.io.{File, FileWriter}
import java.math.BigInteger

import com.google.protobuf.ByteString
import enumeratum.values._
import fr.acinq.bitcoin.DeterministicWallet._
import fr.acinq.bitcoin.MnemonicCode._
import io.circe.syntax._
import io.circe.{Json, Printer}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.protos.node_models
import org.spongycastle.math.ec.ECFieldElement
import scodec.bits._

sealed abstract class KeyType(val value: Int, val name: String) extends IntEnumEntry

object KeyType extends IntEnum[KeyType] {
  case object Master extends KeyType(0, "master")
  case object Issuing extends KeyType(1, "issuing")
  case object Communication extends KeyType(2, "communication")
  case object Authentication extends KeyType(3, "authentication")

  def keyUsage(tpe: KeyType): node_models.KeyUsage = {
    tpe match {
      case Master => node_models.KeyUsage.MASTER_KEY
      case Issuing => node_models.KeyUsage.ISSUING_KEY
      case Communication => node_models.KeyUsage.COMMUNICATION_KEY
      case Authentication => node_models.KeyUsage.AUTHENTICATION_KEY
      case _ => node_models.KeyUsage.UNKNOWN_KEY
    }
  }

  val values = findValues
}

case class DerivedKey(
    tpe: KeyType,
    number: Int,
    keyId: String,
    encoded: String,
    extendedKey: ExtendedPrivateKey
)

case class DerivedDid(number: Int, did: String, keys: List[DerivedKey])

case class Derivation(
    seed: ByteVector,
    seedPhrase: List[String],
    dids: List[DerivedDid]
)

object KeyDerivationTestVectors {

  val JSON_PATH = "key-derivation-test-vectors.json"
  implicit class HardenedInteger(i: Int) {
    def hardened: Long = i.toLong | (1L << 31)
  }

  def deriveKey(
      rootKey: ExtendedPrivateKey,
      didPath: KeyPath,
      tpe: KeyType,
      number: Int
  ): DerivedKey = {
    val keyPath = didPath.derive(tpe.value.hardened).derive(number.hardened)
    val privateKey = derivePrivateKey(rootKey, keyPath)
    privateKey.privateKey
    DerivedKey(
      tpe,
      number,
      s"${tpe.name}-$number",
      encode(privateKey, xprv),
      privateKey
    )
  }

  def bigUnsignedToByteVector(bigUnsigned: BigInteger): ByteVector = {
    ByteVector(bigUnsigned.toByteArray.dropWhile(_ == 0))
  }
  def coordToByteVector(coord: ECFieldElement): ByteVector = {
    bigUnsignedToByteVector(coord.toBigInteger)
  }

  def deriveDid(
      rootKey: ExtendedPrivateKey,
      number: Int,
      keys: Seq[(KeyType, Seq[Int])]
  ): DerivedDid = {
    val didPath = KeyPath.Root.derive(number.hardened)
    val masterKey = deriveKey(rootKey, didPath, KeyType.Master, 0)
    val masterKeyPublic = masterKey.extendedKey.publicKey
    val xCoord = coordToByteVector(masterKeyPublic.ecpoint.getAffineXCoord)
    val yCoord = coordToByteVector(masterKeyPublic.ecpoint.getAffineYCoord)

    val masterKeyProto = node_models.PublicKey(
      id = masterKey.keyId,
      usage = KeyType.keyUsage(masterKey.tpe),
      keyData = node_models.PublicKey.KeyData.EcKeyData(
        node_models.ECKeyData(
          curve = ECConfig.getCURVE_NAME,
          x = ByteString.copyFrom(xCoord.toArray),
          y = ByteString.copyFrom(yCoord.toArray)
        )
      )
    )

    val createOperation = node_models.AtalaOperation(
      operation = node_models.AtalaOperation.Operation.CreateDid(
        node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = Seq(masterKeyProto)
            )
          )
        )
      )
    )

    val didSuffix = Sha256.compute(createOperation.toByteArray).getHexValue
    val did = s"did:prism:$didSuffix"

    val derivedKeys = for {
      (tpe, numbers) <- keys
      number <- numbers
    } yield deriveKey(rootKey, didPath, tpe, number)

    DerivedDid(number, did, derivedKeys.toList)
  }

  def makeDerivation(
      seedPhrase: List[String],
      dids: Seq[(Int, Seq[(KeyType, Seq[Int])])]
  ): Derivation = {
    val seed = toSeed(seedPhrase, "")
    val derivedDids = for {
      (didNumber, keysSpec) <- dids
      rootKey = generate(seed)
    } yield deriveDid(rootKey, didNumber, keysSpec)

    Derivation(seed, seedPhrase, derivedDids.toList)
  }

  def indentLines(s: String, indent: String): String = {
    indent + s.replace("\n", "\n" + indent)
  }

  def derivedKeyToJson(key: DerivedKey): Json = {
    Json.obj(
      "type" -> key.tpe.name.asJson,
      "number" -> key.number.asJson,
      "keyId" -> key.keyId.asJson,
      "bip32path" -> key.extendedKey.path.toString().asJson,
      "secretKey" -> Json.obj(
        "hex" -> bigUnsignedToByteVector(
          key.extendedKey.privateKey.bigInt
        ).toHex.asJson
      ),
      "publicKey" -> Json.obj(
        "hex" -> key.extendedKey.publicKey.value.toHex.asJson,
        "xHex" -> coordToByteVector(
          key.extendedKey.publicKey.ecpoint.getAffineXCoord
        ).toHex.asJson,
        "yHex" -> coordToByteVector(
          key.extendedKey.publicKey.ecpoint.getAffineYCoord
        ).toHex.asJson
      )
    )
  }

  def derivedDidToJson(did: DerivedDid): Json = {
    Json.obj(
      "number" -> did.number.asJson,
      "DID" -> did.did.asJson,
      "keys" -> did.keys.map(derivedKeyToJson).asJson
    )
  }

  def derivationToJson(derivation: Derivation): Json = {
    Json.obj(
      "seedPhrase" -> derivation.seedPhrase.asJson,
      "seedHex" -> derivation.seed.toHex.asJson,
      "dids" -> derivation.dids.map(derivedDidToJson).asJson
    )
  }

  def displayDerivedKey(key: DerivedKey, indent: String): Unit = {
    val s =
      s"""* [Key ${key.tpe.name} ${key.number}]
         |  * Key ID: ${key.keyId}
         |  * BIP32 Path: ${key.extendedKey.path.toString()}
         |  * Secret key
         |    * (hex): ${bigUnsignedToByteVector(
        key.extendedKey.privateKey.bigInt
      ).toHex}
         |  * Public key
         |    * (hex): ${key.extendedKey.publicKey.value.toHex}
         |    * (x-hex): ${coordToByteVector(
        key.extendedKey.publicKey.ecpoint.getAffineXCoord
      ).toHex}
         |    * (y-hex): ${coordToByteVector(
        key.extendedKey.publicKey.ecpoint.getAffineYCoord
      ).toHex}""".stripMargin
    println(indentLines(s, indent))
  }

  def displayDerivedDid(derivedDid: DerivedDid, indent: String): Unit = {
    val s =
      s"""* [DID: ${derivedDid.number}]
         |  * DID: ${derivedDid.did}""".stripMargin

    println(indentLines(s, indent))
    for (key <- derivedDid.keys) {
      displayDerivedKey(key, indent + "  ")
    }
  }

  def displayDerivation(derivation: Derivation): Unit = {
    println(s"Seed phrase: ${derivation.seedPhrase.mkString(" ")}")
    for (did <- derivation.dids) {
      displayDerivedDid(did, "  ")
    }
  }

  def main(args: Array[String]): Unit = {
    val seedPhrase = toMnemonics(hex"000102030405060708090a0b0c0d0e0f")

    val derivation = makeDerivation(
      seedPhrase,
      dids = List(
        1 -> List(
          KeyType.Master -> List(0, 1),
          KeyType.Issuing -> List(5),
          KeyType.Communication -> List(20),
          KeyType.Authentication -> List(27)
        ),
        17 -> List(
          KeyType.Master -> List(0, 17),
          KeyType.Authentication -> List(0, 17)
        )
      )
    )

    displayDerivation(derivation)

    val file = new File(JSON_PATH)
    val writer = new FileWriter(file)
    writer.write(
      Json.arr(derivationToJson(derivation)).printWith(Printer.spaces2SortKeys)
    )
    writer.close()
  }
}
