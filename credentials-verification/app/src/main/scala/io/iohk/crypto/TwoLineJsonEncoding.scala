package io.iohk.crypto

import java.util.Base64

import play.api.libs.json.{Format, Json}

class TwoLineJsonEncoding[T](implicit jsonFormat: Format[T]) extends SignableEncoding[T] {
  override type Enclosure = String

  override def getBytesToSign(enclosure: Enclosure): Array[Byte] = {
    enclosure.getBytes
  }

  override def enclose(t: T): Enclosure = {
    Json.stringify(jsonFormat.writes(t))
  }

  override def disclose(enclosure: Enclosure): T = {
    Json.parse(enclosure).as[T]
  }

  override def compose(enclosure: Enclosure, signature: Array[Byte]): String = {
    require(!enclosure.contains('\n'))

    val signatureString = Base64.getUrlEncoder.encodeToString(signature)

    List(enclosure, "\n", signatureString, "\n").mkString
  }

  override def decompose(value: String): (Enclosure, Array[Byte]) = {
    val enclosureEnd = value.indexOf('\n')
    require(value.indexOf('\n', enclosureEnd + 1) == value.size - 1)

    val enclosure = value.slice(0, enclosureEnd)
    val signature = Base64.getUrlDecoder.decode(value.slice(enclosureEnd + 1, value.size - 1))

    (enclosure, signature)
  }
}
