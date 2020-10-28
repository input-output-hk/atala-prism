package io.iohk.atala.prism

import java.{util => ju}
import java.nio.charset.StandardCharsets

import io.circe.{Decoder, DecodingFailure, Encoder, Json, HCursor, parser}
import io.circe.syntax._

import io.iohk.atala.prism.crypto.ECSignature
import io.iohk.atala.prism.jose.ec.{EcJws, EcJwsContent, EcJwk}

package object jose {

  object implicits {
    implicit val encodeECSignature: Encoder[ECSignature] = (s: ECSignature) => {
      ju.Base64.getUrlEncoder.withoutPadding
        .encodeToString(s.toP1363.data)
        .asJson
    }

    implicit val encodeEcJwk: Encoder[EcJwk] = (ecJwk: EcJwk) => {

      Json
        .obj(
          "crv" -> ecJwk.crv.asJson,
          "x" -> ecJwk.x.asJson,
          "y" -> ecJwk.y.asJson,
          "kid" -> ecJwk.kid.asJson,
          "kty" -> ecJwk.kty.asJson
        )
        .dropNullValues
    }

    implicit val encodeJwsHeader: Encoder[JwsHeader[EcJwk]] = (header: JwsHeader[EcJwk]) => {
      Json
        .obj(
          "alg" -> header.alg.alg.asJson,
          "jku" -> header.jku.asJson,
          "jwk" -> header.jwk.asJson,
          "kid" -> header.kid.asJson,
          "x5u" -> header.x5u.asJson,
          "x5c" -> header.x5c.asJson,
          "x5t" -> header.x5t.asJson,
          "x5t#S256" -> header.`x5t#S256`.asJson,
          "typ" -> header.typ.asJson,
          "name" -> header.name.asJson,
          "b64" -> header.b64.asJson,
          "crit" -> header.crit.asJson
        )
        .dropNullValues
    }

    implicit def encodeJwsSignature[S: Encoder]: Encoder[JwsSignature[S]] =
      (s: JwsSignature[S]) => {
        Json
          .obj(
            "protected" -> s.`protected`.asJson,
            "signature" -> s.signature.asJson
          )
      }

    implicit def encodeEcJws[P: Encoder]: Encoder[EcJws[P]] =
      (jws: EcJws[P]) => {
        Json.obj(
          "payload" -> jws.content.encodedPayload.asJson,
          "signatures" -> jws.signatures.asJson
        )
      }

    implicit val decodeECSignature: Decoder[ECSignature] = (c: HCursor) => {
      for {
        signatute <- c.as[String].map(ju.Base64.getUrlDecoder.decode)
      } yield ECSignature(signatute).toDer
    }

    implicit def decodeEcJwk: Decoder[EcJwk] =
      (c: HCursor) => {
        for {
          x <- c.get[String]("x")
          y <- c.get[String]("y")
          kid <- c.get[Option[String]]("kid")
        } yield EcJwk(
          crv = "secp256k1",
          x = x,
          y = y,
          didId = kid
        )
      }

    implicit val decodeJwa: Decoder[Jwa] = (c: HCursor) => {
      c.as[String] match {
        case Left(value) => Left(value)
        case Right(value) if value == "ES256K" => Right(Jwa.ES256K)
      }
    }

    implicit def decodeJwsHeader: Decoder[JwsHeader[EcJwk]] =
      (c: HCursor) => {
        for {
          alg <- c.get[Jwa]("alg")
          jku <- c.get[Option[String]]("jku")
          jwk <- c.get[EcJwk]("jwk")
          kid <- c.get[Option[String]]("kid")
          x5u <- c.get[Option[String]]("x5u")
          x5c <- c.get[Option[String]]("x5c")
          x5t <- c.get[Option[String]]("x5t")
          `x5t#S256` <- c.get[Option[String]]("x5t#S256")
          typ <- c.get[Option[String]]("typ")
          name <- c.get[Option[String]]("name")
          b64 <- c.get[Option[Boolean]]("b64")
          crit <- c.get[Option[Seq[String]]]("crit")
        } yield JwsHeader(
          alg = alg,
          jku = jku,
          jwk = jwk,
          kid = kid,
          x5u = x5u,
          x5c = x5c,
          x5t = x5t,
          `x5t#S256` = `x5t#S256`,
          typ = typ,
          name = name,
          b64 = b64,
          crit = crit
        )
      }

    implicit def decodeJwsSignature[S: Decoder]: Decoder[JwsSignature[S]] =
      (c: HCursor) => {
        for {
          `protected` <- c.get[String]("protected")
          signature <- c.get[S]("signature")
        } yield JwsSignature(`protected`, signature)
      }

    implicit def decodeEcJws[P: Decoder: Encoder]: Decoder[EcJws[P]] =
      (c: HCursor) => {
        for {
          payload <-
            c.get[String]("payload")
              .flatMap(parser.decode[P])
              .left
              .map(e => DecodingFailure(e.getMessage, Nil))
          protectedHeader <-
            c.downField("signatures")
              .downArray
              .get[String]("protected")
              .map(h => new String(ju.Base64.getUrlDecoder.decode(h), StandardCharsets.UTF_8))
              .flatMap(parser.decode[JwsHeader[EcJwk]])
              .left
              .map(e => DecodingFailure(e.getMessage, Nil))
          signatures <- c.get[Seq[JwsSignature[ECSignature]]]("signatures")
        } yield {
          new EcJws(EcJwsContent(protectedHeader, payload), signatures)
        }
      }
  }

}
