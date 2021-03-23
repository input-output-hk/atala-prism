package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import org.slf4j.{Logger, LoggerFactory}

class DIDDataRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def findByDid(did: DID): FutureEither[NodeError, DIDDataState] = {
    val query = for {
      didSuffix <- OptionT(connection.pure(did.getCanonicalSuffix))
        .toRight[NodeError](UnknownValueError("did", did.value))
      lastOperation <- OptionT(DIDDataDAO.getLastOperation(didSuffix))
        .toRight[NodeError](UnknownValueError("didSuffix", didSuffix.value))
      keys <- EitherT.right[NodeError](PublicKeysDAO.findAll(didSuffix))
    } yield DIDDataState(didSuffix, keys, lastOperation)

    query.value
      .logSQLErrors(s"finding, did - $did", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }
}
