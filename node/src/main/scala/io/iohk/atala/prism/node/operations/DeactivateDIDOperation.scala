package io.iohk.atala.prism.node.operations

import cats.data.EitherT
import doobie.free.connection.{ConnectionIO, unit}
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{KeyUsage, nodeState}
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, ServicesDAO, ContextDAO}
import io.iohk.atala.prism.protos.node_models.AtalaOperation

case class DeactivateDIDOperation(
    didSuffix: DidSuffix,
    previousOperation: Sha256Digest,
    digest: Sha256Digest,
    ledgerData: nodeState.LedgerData
) extends Operation {
  override val metricCounterName: String = DeactivateDIDOperation.metricCounterName

  override def linkedPreviousOperation: Option[Sha256Digest] = Some(
    previousOperation
  )

  /** Fetches key and possible previous operation reference from database */
  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      lastOperation <- EitherT[ConnectionIO, StateError, Sha256Digest] {
        DIDDataDAO
          .getLastOperation(didSuffix)
          .map(
            _.toRight(
              StateError.EntityMissing("did suffix", didSuffix.getValue)
            )
          )
      }
      key <- EitherT[ConnectionIO, StateError, DIDPublicKeyState] {
        PublicKeysDAO
          .find(didSuffix, keyId)
          .map(_.toRight(StateError.UnknownKey(didSuffix, keyId)))
      }.subflatMap { didKey =>
        Either.cond(
          didKey.keyUsage == KeyUsage.MasterKey,
          didKey,
          StateError.InvalidKeyUsed("master key")
        )
      }.subflatMap { didKey =>
        Either.cond(
          didKey.revokedOn.isEmpty,
          didKey,
          StateError.KeyAlreadyRevoked()
        )
      }.map(_.key)
    } yield CorrectnessData(key, Some(lastOperation))
  }

  override protected def applyStateImpl(c: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] = {
    for {
      countUpdated <- EitherT.right(
        DIDDataDAO.updateLastOperation(didSuffix, digest)
      )
      _ <- EitherT.cond[ConnectionIO](
        countUpdated == 1,
        unit,
        StateError.EntityMissing("DID Suffix", didSuffix.getValue)
      )

      _ <- EitherT.right[StateError](PublicKeysDAO.revokeAllKeys(didSuffix, ledgerData))

      _ <- EitherT.right[StateError](ServicesDAO.revokeAllServices(didSuffix, ledgerData))

      _ <- EitherT.right[StateError](ContextDAO.revokeAllContextStrings(didSuffix, ledgerData))
    } yield ()
  }
}

object DeactivateDIDOperation extends OperationCompanion[DeactivateDIDOperation] {
  val metricCounterName: String = "number_of_did_deactivates"

  override protected def parse(
      operation: AtalaOperation,
      ledgerData: nodeState.LedgerData
  ): Either[ValidationError, DeactivateDIDOperation] = {
    val operationDigest = Sha256.compute(operation.toByteArray)
    val deactivateOperation =
      ValueAtPath(operation, Path.root).child(_.getDeactivateDid, "deactivateDid")

    for {
      didSuffix <- deactivateOperation.child(_.id, "id").parse { didSuffix =>
        DidSuffix.fromString(didSuffix).toEither.left.map(_.getMessage)
      }
      previousOperation <- ParsingUtils.parseHash(
        deactivateOperation.child(_.previousOperationHash, "previousOperationHash")
      )
    } yield DeactivateDIDOperation(
      didSuffix,
      previousOperation,
      operationDigest,
      ledgerData
    )
  }
}
