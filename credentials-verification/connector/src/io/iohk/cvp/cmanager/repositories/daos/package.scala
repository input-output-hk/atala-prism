package io.iohk.cvp.cmanager.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.{Get, Put}
import io.iohk.cvp.cmanager.models.{Credential, Issuer, IssuerGroup, Student}

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

}
