package io.iohk.atala.prism.utils

import doobie.Meta
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.parse
import org.postgresql.util.PGobject
import cats.implicits._
import io.circe.syntax._

import scala.collection.compat.immutable.ArraySeq

object DoobieImplicits {
  implicit val byteArraySeqMeta: Meta[ArraySeq[Byte]] =
    Meta[Array[Byte]].timap {
      ArraySeq.unsafeWrapArray
    } { _.toArray }

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(a => {
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      })

  def circeMeta[A: Encoder: Decoder]: Meta[A] =
    Meta[Json].timap[A](
      _.as[A].fold[A](
        e => sys.error(s"Cannot parse json: ${e.message}"),
        identity
      )
    )(_.asJson)
}
