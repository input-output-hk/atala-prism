package io.iohk.atala.prism.interop

import io.iohk.atala.prism.credentials.content.CredentialContent
import scala.jdk.CollectionConverters._
import io.circe.syntax.EncoderOps

object CredentialContentConverter {

  implicit class JsonElementConv(val v: CredentialContent) extends AnyVal {
    def asString: String = {
      val map: Map[String, String] =
        v.getFields.getKeys.asScala
          .zip(
            v.getFields.getValues.asScala.map(_.toString.drop(1).dropRight(1))
          )
          .toMap

      map.asJson.spaces2
    }
  }

}
