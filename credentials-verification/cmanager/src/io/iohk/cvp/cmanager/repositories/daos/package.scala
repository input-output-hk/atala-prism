package io.iohk.cvp.cmanager.repositories

import java.util.UUID

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Put, Read}
import io.iohk.cvp.cmanager.models.{Credential, Issuer}

package object daos {

  implicit val credentialIdPut: Put[Credential.Id] = Put[UUID].contramap(_.value)
  implicit val issuerIdPut: Put[Issuer.Id] = Put[UUID].contramap(_.value)

}
