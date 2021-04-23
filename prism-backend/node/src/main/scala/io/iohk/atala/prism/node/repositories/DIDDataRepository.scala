package io.iohk.atala.prism.node.repositories

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
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

import scala.concurrent.Future

class DIDDataRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def findByDid(did: DID): FutureEither[NodeError, Option[DIDDataState]] = {
    did.getCanonicalSuffix match {
      case Some(didSuffix) =>
        val query = for {
          lastOperationMaybe <- DIDDataDAO.getLastOperation(didSuffix)
          keys <- PublicKeysDAO.findAll(didSuffix)
        } yield lastOperationMaybe map { lastOperation =>
          DIDDataState(didSuffix, keys, lastOperation)
        }

        EitherT
          .right(query)
          .value
          .logSQLErrors(s"finding, did - $did", logger)
          .transact(xa)
          .unsafeToFuture()
          .toFutureEither
      case None =>
        logger.info(s"Unknown DID format: $did")
        Future.successful(UnknownValueError("did", did.value).asLeft).toFutureEither
    }
  }
}
