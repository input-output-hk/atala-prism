package io.iohk.cvp.cstore.repositories

import doobie.postgres.implicits._
import doobie.util.Meta
import doobie.util.invariant.InvalidEnum
import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, Verifier}
import io.iohk.cvp.daos.BaseDAO

package object daos extends BaseDAO {

  implicit val verifierIdMeta: Meta[Verifier.Id] = uuidMeta.timap(Verifier.Id.apply)(_.uuid)

  implicit val pgPackageTypeMeta: Meta[IndividualConnectionStatus] = pgEnumString[IndividualConnectionStatus](
    "INDIVIDUAL_CONNECTION_STATUS_TYPE",
    a => IndividualConnectionStatus.withNameOption(a).getOrElse(throw InvalidEnum[IndividualConnectionStatus](a)),
    _.entryName
  )
}
