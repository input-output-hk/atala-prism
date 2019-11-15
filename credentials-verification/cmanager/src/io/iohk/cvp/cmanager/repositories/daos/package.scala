package io.iohk.cvp.cmanager.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.Put
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}

package object daos {

  implicit val credentialIdPut: Put[Credential.Id] = Put[UUID].contramap(_.value)
  implicit val issuerIdPut: Put[Issuer.Id] = Put[UUID].contramap(_.value)
  implicit val studentIdPut: Put[Student.Id] = Put[UUID].contramap(_.value)
  implicit val studentConnectionStatusPut: Put[Student.ConnectionStatus] = Put[String].contramap(_.entryName)

}
