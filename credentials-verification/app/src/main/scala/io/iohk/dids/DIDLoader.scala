package io.iohk.dids

import play.api.libs.json.Json

import scala.util.Try

object DIDLoader {

  def getDID(path: os.ReadablePath): Try[Document] =
    Try {
      val string = os.read(path)
      Json.parse(string).as[Document]
    }

  def getJWKPrivate(path: os.ReadablePath): Try[JWKPrivateKey] =
    Try {
      val string = os.read(path)
      Json.parse(string).as[JWKPrivateKey]
    }
}
