package io.iohk.atala.prism.cstore.repositories

import doobie.postgres.implicits._
import doobie.util.Meta
import io.iohk.atala.prism.cstore.models.IndividualConnectionStatus
import io.iohk.atala.prism.daos.BaseDAO

package object daos extends BaseDAO {

  implicit val pgPackageTypeMeta: Meta[IndividualConnectionStatus] = pgEnumString[IndividualConnectionStatus](
    "INDIVIDUAL_CONNECTION_STATUS_TYPE",
    IndividualConnectionStatus.fromContactStatus,
    IndividualConnectionStatus.toContactStatus
  )
}
