package io.iohk.atala.prism.node.cardano.dbsync.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.atala.prism.node.cardano.models.{Block, BlockError}
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

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
}
