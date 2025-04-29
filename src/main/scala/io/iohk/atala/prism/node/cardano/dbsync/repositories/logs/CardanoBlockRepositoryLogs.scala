package io.iohk.atala.prism.node.cardano.dbsync.repositories.logs

import cats.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.Block
import io.iohk.atala.prism.node.cardano.models.BlockError
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class CardanoBlockRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  CardanoBlockRepository[F]
]: MonadThrow]
    extends CardanoBlockRepository[Mid[F, *]] {
  override def getFullBlock(
      blockNo: Int
  ): Mid[F, Either[BlockError.NotFound, Block.Full]] =
    in =>
      info"getting full block $blockNo" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while getting full block $blockNo: $er",
            fb => info"getting full block with header ${fb.header} - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while getting full block" (_))

  override def getLatestBlock: Mid[F, Either[BlockError.NoneAvailable.type, Block.Canonical]] =
    in =>
      info"getting latest block" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while getting latest block $er",
            res => info"getting latest block with header ${res.header} - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while getting latest block" (_)
        )

  override def getAllPrismIndexBlocksWithTransactions(): Mid[F, Either[BlockError.NotFound, List[Block.Full]]] =
    in =>
      info"getting all prism index blocks with transactions" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while getting all prism index blocks with transactions: $er",
            res => info"getting all prism index blocks with headers ${res.map(_.header)} - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while getting all prism index blocks with transactions" (_))
}
