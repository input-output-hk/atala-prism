package io.iohk.atala.prism.node.client

import java.io.{BufferedWriter, File, FileWriter}
import java.util.Base64

import io.iohk.atala.crypto.{EC, ECConfig, ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.prism.protos.node_models

import scala.io.Source

case class State(
    keys: List[(String, node_models.KeyUsage, ECPrivateKey, ECPublicKey)] = List.empty,
    lastOperationPerId: Map[String, SHA256Digest] = Map.empty,
    didSuffix: Option[String] = None
)

object StateStorage {
  val DEFAULT_STORAGE = new File("client-storage.txt")

  val decoder = Base64.getUrlDecoder
  val encoder = Base64.getUrlEncoder

  def load(path: File): State = {
    if (!path.exists()) {
      println(s"$path does not exist - not loading state")
      State()
    } else {
      println(s"Loading state from $path")
      val source = Source.fromFile(path)
      val splitLines =
        try {
          source.getLines().filter(_.trim.nonEmpty).map(_.split(" ")).toList
        } finally {
          source.close()
        }

      val didSuffix = splitLines.collectFirst {
        case Array("did-suffix", didSuffix) => didSuffix
      }

      val keys = splitLines.collect {
        case Array("key", keyId, usage, curve, d) =>
          assert(curve == ECConfig.CURVE_NAME)
          val dBytes = decoder.decode(d)
          (
            keyId,
            node_models.KeyUsage.fromName(usage).get,
            EC.toPrivateKey(dBytes),
            EC.toPublicKeyFromPrivateKey(dBytes)
          )
      }

      val operations = splitLines.collect {
        case Array("operation", id, lastOperation) =>
          (id, SHA256Digest.fromHex(lastOperation))
      }.toMap

      State(keys = keys, lastOperationPerId = operations, didSuffix = didSuffix)
    }
  }

  def save(path: File, state: State): Unit = {
    val writer = new BufferedWriter(new FileWriter(path))

    try {
      for (didSuffix <- state.didSuffix) {
        writer.write(s"did-suffix $didSuffix\n")
      }

      // public key is not stored, as it can be derived from private one
      for ((keyId, usage, key, _) <- state.keys) {
        assert(!keyId.contains(" "))
        val dStr = encoder.encodeToString(key.getEncoded)
        writer.write(s"key $keyId ${usage.toString()} ${ECConfig.CURVE_NAME} $dStr\n")
      }

      for ((id, lastOperation) <- state.lastOperationPerId) {
        writer.write(s"operation $id ${lastOperation.hexValue}\n")
      }
    } finally {
      writer.close()
    }
  }
}
