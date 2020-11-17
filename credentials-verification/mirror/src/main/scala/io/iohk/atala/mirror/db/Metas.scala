package io.iohk.atala.mirror.db

import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.parse
import org.postgresql.util.PGobject
import cats.implicits._
import scala.reflect.runtime.universe.TypeTag
import io.circe.syntax._

object Metas {

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(a => {
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      })

  def circeMeta[A: Encoder: Decoder: TypeTag]: Meta[A] =
    Meta[Json].timap[A](
      _.as[A].fold[A](
        e => sys.error(s"Cannot parse json: ${e.message}"),
        identity
      )
    )(_.asJson)

}
