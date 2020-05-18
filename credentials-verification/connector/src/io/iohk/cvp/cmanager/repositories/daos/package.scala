package io.iohk.cvp.cmanager.repositories

import java.util.UUID

import cats.syntax.either.catsSyntaxEither
import doobie.postgres.implicits._
import doobie.util.{Get, Meta, Put}
import io.circe._
import io.circe.parser._
import io.iohk.cvp.cmanager.models._
import org.postgresql.util.PGobject

package object daos {

  implicit val uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  implicit val credentialIdPut: Put[Credential.Id] = Put[UUID].contramap(_.value)

  implicit val issuerIdPut: Put[Issuer.Id] = Put[UUID].contramap(_.value)
  implicit val issuerIdGet: Get[Issuer.Id] = Get[UUID].map(Issuer.Id.apply)

  implicit val studentIdPut: Put[Student.Id] = Put[UUID].contramap(_.value)
  implicit val studentIdGet: Get[Student.Id] = Get[UUID].map(Student.Id.apply)

  implicit val studentConnectionStatusPut: Put[Student.ConnectionStatus] = Put[String].contramap(_.entryName)
  implicit val studentConnectionStatusGet: Get[Student.ConnectionStatus] = {
    Get[String].map(Student.ConnectionStatus.withNameInsensitive)
  }

  implicit val groupIdPut: Put[IssuerGroup.Id] = Put[UUID].contramap(_.value)
  implicit val groupIdGet: Get[IssuerGroup.Id] = Get[UUID].map(IssuerGroup.Id.apply)

  implicit val groupNamePut: Put[IssuerGroup.Name] = Put[String].contramap(_.value)
  implicit val groupNameGet: Get[IssuerGroup.Name] = Get[String].map(IssuerGroup.Name.apply)

  implicit val subjectIdMeta: Meta[Subject.Id] = Meta[UUID].timap(Subject.Id.apply)(_.value)

  // Copied from the official docs https://tpolecat.github.io/doobie/docs/12-Custom-Mappings.html#defining-get-and-put-for-exotic-types
  implicit val jsonMeta: Meta[Json] = Meta.Advanced
    .other[PGobject]("json")
    .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(a => {
      val o = new PGobject
      o.setType("json")
      o.setValue(a.noSpaces)
      o
    })
}
