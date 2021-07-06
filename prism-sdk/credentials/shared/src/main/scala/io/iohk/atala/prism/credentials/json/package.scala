package io.iohk.atala.prism.credentials

import io.circe.syntax._
import io.circe.{Encoder, Decoder, Json, HCursor, DecodingFailure}

import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.CredentialContent._

package object json {

  private[json] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }

  /**
    * Circle JSON encoders/decoders.
    */
  object implicits {
    implicit def encodeValue: Encoder[Value] = {
      Encoder.instance {
        case StringValue(value) => value.asJson
        case IntValue(value) => value.asJson
        case BooleanValue(value) => value.asJson
        case SeqValue(values) => values.asJson
        case SubFields(fields) => fields.map(_.asJson).fold(Json.obj())(_ deepMerge _)
      }
    }

    implicit def encodeField: Encoder[Field] = field => Json.obj(field.name -> field.value.asJson)

    implicit def encodeCredentialContent: Encoder[CredentialContent] =
      _.fields.map(_.asJson).fold(Json.obj())(_ deepMerge _)

    // TODO: Change the code to make sure that every Value is encoded.
    implicit def decodeValue: Decoder[Value] = {
      Decoder[String].map[Value](StringValue(_)) or
        Decoder[Int].map[Value](IntValue(_)) or
        Decoder[Values].map[Value](SeqValue(_)) or
        Decoder[Fields].map[Value](SubFields(_))
    }

    implicit def decodeFields: Decoder[Fields] = { (c: HCursor) =>
      {
        for {
          keys <- c.keys.toRight(DecodingFailure(s"Wrong credential keys $c", Nil))
          fields <- keys.partitionMap(key => c.get[Value](key).map(Field(key, _))) match {
            case (Nil, rights) => Right(rights)
            case (firstLeft, _) => Left(firstLeft.head)
          }
        } yield fields.toIndexedSeq
      }
    }

    implicit def decodeCredentialContent: Decoder[CredentialContent] =
      decodeFields(_).map(CredentialContent(_))
  }

}
