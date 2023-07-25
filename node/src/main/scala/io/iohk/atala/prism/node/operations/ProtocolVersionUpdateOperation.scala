package io.iohk.atala.prism.node.operations
import cats.data.EitherT
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits.toDoobieApplicativeErrorOps
import doobie.postgres.sqlstate
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_NO
import io.iohk.atala.prism.node.models.KeyUsage.MasterKey
import io.iohk.atala.prism.node.models.nodeState._
import io.iohk.atala.prism.node.models.{ProtocolVersion, ProtocolVersionInfo}
import io.iohk.atala.prism.node.operations.StateError._
import io.iohk.atala.prism.node.operations.path.{Path, ValueAtPath}
import io.iohk.atala.prism.node.repositories.daos.{KeyValuesDAO, ProtocolVersionsDAO, PublicKeysDAO}
import io.iohk.atala.prism.protos.{node_models => proto}

case class ProtocolVersionUpdateOperation(
    versionName: Option[String],
    protocolVersion: ProtocolVersion,
    effectiveSinceBlockIndex: Int,
    proposerDID: DidSuffix,
    override val digest: Sha256Digest,
    override val ledgerData: LedgerData
) extends Operation {
  override val metricCounterName: String = ProtocolVersionUpdateOperation.metricCounterName

  override def getCorrectnessData(
      keyId: String
  ): EitherT[ConnectionIO, StateError, CorrectnessData] = {
    for {
      keyState <- EitherT {
        PublicKeysDAO
          .find(proposerDID, keyId)
          .map(_.toRight(StateError.UnknownKey(proposerDID, keyId)))
      }

      _ <- EitherT.fromEither[ConnectionIO] {
        Either.cond(
          keyState.revokedOn.isEmpty,
          (),
          StateError.KeyAlreadyRevoked()
        )
      }

      data <- EitherT.fromEither[ConnectionIO] {
        Either.cond(
          keyState.keyUsage == MasterKey,
          CorrectnessData(keyState.key, None),
          StateError.InvalidKeyUsed(
            s"The key type expected is Master key. Type used: ${keyState.keyUsage}"
          ): StateError
        )
      }
    } yield data
  }

  override protected def applyStateImpl(config: ApplyOperationConfig): EitherT[ConnectionIO, StateError, Unit] =
    for {
      _ <- EitherT.cond[ConnectionIO](
        proposerDID == config.trustedProposer,
        (),
        UntrustedProposer(proposerDID): StateError
      )

      lastKnown <- EitherT.liftF(ProtocolVersionsDAO.getLastKnownProtocolUpdate)
      _ <-
        EitherT
          .cond[ConnectionIO](
            lastKnown.protocolVersion isFollowedBy protocolVersion,
            (),
            NonSequentialProtocolVersion(
              lastKnown.protocolVersion,
              protocolVersion
            ): StateError
          )

      _ <- EitherT.cond[ConnectionIO](
        effectiveSinceBlockIndex > lastKnown.effectiveSinceBlockIndex,
        (),
        NonAscendingEffectiveSince(
          lastKnown.effectiveSinceBlockIndex,
          effectiveSinceBlockIndex
        ): StateError
      )

      lastBlockNo <- EitherT.liftF(
        KeyValuesDAO
          .get(LAST_SYNCED_BLOCK_NO)
          .map(_.fold(0)(_.value.map(_.toInt).getOrElse(0)))
      )
      _ <- EitherT.cond[ConnectionIO](
        effectiveSinceBlockIndex > lastBlockNo,
        (),
        EffectiveSinceNotGreaterThanCurrentCardanoBlockNo(
          lastBlockNo,
          effectiveSinceBlockIndex
        ): StateError
      )

      _ <- EitherT[ConnectionIO, StateError, Unit] {
        ProtocolVersionsDAO
          .insertProtocolVersion(
            protocolVersion,
            versionName,
            effectiveSinceBlockIndex,
            proposerDID,
            ledgerData
          )
          .attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION =>
              EntityExists(
                "Protocol version",
                protocolVersion.toString
              ): StateError
            case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
              UntrustedProposer(proposerDID)
          }
      }
    } yield ()

  def toProtocolVersionInfo: ProtocolVersionInfo =
    ProtocolVersionInfo(protocolVersion, versionName, effectiveSinceBlockIndex)
}

object ProtocolVersionUpdateOperation extends SimpleOperationCompanion[ProtocolVersionUpdateOperation] {
  val metricCounterName: String = "number_of_protocol_updates"

  override def parse(
      operation: proto.AtalaOperation,
      ledgerData: LedgerData
  ): Either[ValidationError, ProtocolVersionUpdateOperation] = {
    val operationDigest = Sha256.compute(operation.toByteArray)
    val updateProtocolOperation =
      ValueAtPath(operation, Path.root)
        .child(_.getProtocolVersionUpdate, "protocolVersionUpdate")
    for {
      proposerDIDSuffix <- updateProtocolOperation
        .child(_.proposerDid, "proposerDid")
        .parse { proposerDID =>
          DidSuffix.fromString(proposerDID).toEither.left.map(_.getMessage)
        }
      versionInfo <- updateProtocolOperation.childGet(_.version, "version")
      versionName <- versionInfo.child(_.versionName, "versionName").parse { name =>
        if (name.isEmpty) None.asRight
        else Some(name).asRight
      }
      protocolVersion <-
        versionInfo
          .childGet(_.protocolVersion, "protocolVersion")
      major <-
        protocolVersion
          .child(_.majorVersion, "majorVersion")
          .parse(v => Either.cond(v >= 0, v, "Negative major version"))
      minor <-
        protocolVersion
          .child(_.minorVersion, "minorVersion")
          .parse(v => Either.cond(v >= 0, v, "Negative minor version"))
      effectiveSince <-
        versionInfo
          .child(_.effectiveSince, "effectiveSince")
          .parse(e => Either.cond(e > 0, e, "Negative or zero effectiveSince"))
    } yield ProtocolVersionUpdateOperation(
      versionName,
      ProtocolVersion(major, minor),
      effectiveSince,
      proposerDIDSuffix,
      operationDigest,
      ledgerData
    )
  }
}
