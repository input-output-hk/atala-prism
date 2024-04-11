package io.iohk.atala.prism.node.models

import com.typesafe.config.ConfigMemorySize

import java.util.Locale
import scala.collection.compat.immutable.ArraySeq
import scala.util.matching.Regex
import io.iohk.atala.prism.node.utils.BytesOps


trait HashValue extends Any {

  def value: ArraySeq[Byte]

  override def toString: String = {
    BytesOps.bytesToHex(value)
  }
}

trait HashValueFrom[A] {
  protected val config: HashValueConfig

  protected def constructor(value: ArraySeq[Byte]): A

  def from(string: String): Option[A] = {
    val lowercaseString = string.toLowerCase(Locale.ROOT)

    lowercaseString match {
      case config.HexPattern() =>
        val bytes = lowercaseString
          .grouped(2)
          .toList
          .map { hex =>
            Integer.parseInt(hex, 16).asInstanceOf[Byte]
          }
        Some(constructor(ArraySeq.from(bytes)))
      case _ => None
    }
  }

  def from(bytes: Iterable[Byte]): Option[A] = {
    if (bytes.size == config.size.toBytes) {
      Some(constructor(ArraySeq.from(bytes)))
    } else {
      None
    }
  }
}

case class HashValueConfig(size: ConfigMemorySize) {
  private[models] val HexPattern: Regex = s"^[a-f0-9]{${2 * size.toBytes}}$$".r
}
