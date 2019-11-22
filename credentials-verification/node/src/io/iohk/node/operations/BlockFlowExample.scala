package io.iohk.node.operations

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.node.operations.ValidationError.InvalidValue
import io.iohk.node.operations.path._
import io.iohk.node.repositories.daos.PublicKeysDAO
import io.iohk.nodenew.{atala_bitcoin_new => atala_proto, geud_node_new => geud_proto}

object BlockFlowExample {

  case class ParsedOperation[Repr <: Operation](_operation: Repr, raw: geud_proto.SignedAtalaOperation) {
    def operation: Operation = _operation
  }

  def parseOperation(signedOperation: geud_proto.SignedAtalaOperation): Either[ValidationError, ParsedOperation[_]] = {
    signedOperation.getOperation.operation match {
      case _: geud_proto.AtalaOperation.Operation.CreateDid =>
        CreateDIDOperation
          .parse(signedOperation.getOperation)
          .map(repr => ParsedOperation(repr, signedOperation))
      case op =>
        Left(InvalidValue(Path.root, op.getClass.getSimpleName, "Unknown operation"))
    }
  }

  def processBlock(block: atala_proto.AtalaBlock, xa: Transactor[IO]): Unit = {
    type EitherErrorOr[T] = Either[ValidationError, T]

    val reverseParsedOperations = block.operations
      .foldLeft[Either[ValidationError, List[ParsedOperation[_]]]](Right(List.empty)) { (acc, signedOp) =>
        acc.flatMap(list => parseOperation(signedOp).map(_ :: list))
      }

    val parsedOperations = reverseParsedOperations.map(_.reverse)

    parsedOperations.foreach(_.foreach { parsedOperation =>
      val encoded = parsedOperation.raw.getOperation.toByteArray
      val keyET = EitherT
        .fromEither[ConnectionIO](parsedOperation.operation.getKey(parsedOperation.raw.signedWith))
        .flatMap {
          case OperationKey.IncludedKey(k) => EitherT.rightT[ConnectionIO, StateError](k)
          case OperationKey.DeferredKey(owner, keyId) =>
            OptionT(PublicKeysDAO.find(owner, keyId))
              .toRight[StateError](StateError.UnknownKey(owner, keyId))
              .map(_.key)
        }

      for {
        key <- keyET
        // verify signature here, we can do that as we have the key and encoded operation
        _ <- parsedOperation.operation.applyState()
      } yield ()

    })

  }
}
