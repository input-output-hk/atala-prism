package io.iohk.cvp.cstore.repositories

import doobie.postgres.implicits.pgEnumString
import doobie.util.invariant.InvalidEnum
import doobie.util.{Get, Put}
import io.iohk.connector.model.ConnectionId
import io.iohk.cvp.cstore.models.IndividualConnectionStatus
import io.iohk.cvp.models.ParticipantId

package object daos {
  implicit val participantIdPut: Put[ParticipantId] = io.iohk.connector.repositories.daos.participantIdPut
  implicit val participantIdGet: Get[ParticipantId] = io.iohk.connector.repositories.daos.participantIdGet

  implicit val connectionIdPut: Put[ConnectionId] = io.iohk.connector.repositories.daos.connectionIdPut
  implicit val connectionIdGet: Get[ConnectionId] = io.iohk.connector.repositories.daos.connectionIdGet

  implicit val pgPackageTypeMeta = pgEnumString[IndividualConnectionStatus](
    "INDIVIDUAL_CONNECTION_STATUS_TYPE",
    a => IndividualConnectionStatus.withNameOption(a).getOrElse(throw InvalidEnum[IndividualConnectionStatus](a)),
    _.entryName
  )

  implicit val csput = implicitly[Put[IndividualConnectionStatus]]
}
