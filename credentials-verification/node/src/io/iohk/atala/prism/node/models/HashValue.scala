package io.iohk.atala.prism.node.models

import java.util.Locale

import com.typesafe.config.ConfigMemorySize
import javax.xml.bind.DatatypeConverter

import scala.collection.compat.immutable.ArraySeq
import scala.util.matching.Regex

trait HashValue extends Any {

  def value: ArraySeq[Byte]

  override def toString: String = {
    DatatypeConverter.printHexBinary(value.toArray).toLowerCase(Locale.ROOT)
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
        Some(constructor(ArraySeq.unsafeWrapArray(bytes.toArray)))
      case _ => None
    }
  }

  def from(bytes: Seq[Byte]): Option[A] = {
    if (bytes.length == config.size.toBytes) {
      Some(constructor(ArraySeq.unsafeWrapArray(bytes.toArray)))
    } else {
      None
    }
  }
}

case class HashValueConfig(size: ConfigMemorySize) {
  private[models] val HexPattern: Regex = s"^[a-f0-9]{${2 * size.toBytes}}$$".r
}
