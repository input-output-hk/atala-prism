package io.iohk.atala.prism.intdemo

import cats.effect.{IO, Timer}
import cats.syntax.functor._
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.intdemo.IntDemoStateMachine.log
import io.iohk.atala.prism.connector.model.{Connection, ConnectionId, TokenString}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.intdemo.protos.{intdemo_api, intdemo_models}
import io.iohk.atala.prism.protos.credential_models
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IntDemoStateMachine[D](
    requiredDataLoader: TokenString => Future[Option[D]],
    getCredential: D => credential_models.PlainTextCredential,
    proofRequestIssuer: Connection => Future[Unit],
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    connectionToken: TokenString,
    issuerId: ParticipantId
)(implicit ec: ExecutionContext) {
  implicit val timer: Timer[IO] = IO.timer(ec)

  def getCurrentStatus(): Future[intdemo_models.SubjectStatus] = {
    // Detect if any changes to the "old" status need to be applied, and apply them before returning the current status
    for {
      oldStatus <- getOldStatus()
      requiredData <- requiredDataLoader(connectionToken)
      connection <- getWalletConnection()
      nextStatus <- State.stateMap(oldStatus)(
        oldStatus,
        requiredData,
        connection
      )
      _ <- setNextState(oldStatus, nextStatus)
    } yield nextStatus
  }

  def streamCurrentStatus(
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse],
      schedulerPeriod: FiniteDuration
  ): Unit = {
    getCurrentStatus().onComplete {
      case Success(status) =>
        if (status == intdemo_models.SubjectStatus.CREDENTIAL_SENT) {
          complete(
            intdemo_api.GetSubjectStatusResponse(status),
            responseObserver
          )
        } else {
          next(
            intdemo_api.GetSubjectStatusResponse(status),
            responseObserver,
            schedulerPeriod
          )
        }
      case Failure(exception) =>
        error(exception, responseObserver)
    }
  }

  private def next(
      response: intdemo_api.GetSubjectStatusResponse,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse],
      schedulerPeriod: FiniteDuration
  ): Unit = {
    try {
      responseObserver.onNext(response)
      (IO.sleep(schedulerPeriod) *> IO(streamCurrentStatus(responseObserver, schedulerPeriod)))
        .unsafeRunAsyncAndForget()
      ()
    } catch (withLoggingHandler)
  }

  private def complete(
      response: intdemo_api.GetSubjectStatusResponse,
      responseObserver: StreamObserver[intdemo_api.GetSubjectStatusResponse]
  ): Unit = {
    try {
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (withLoggingHandler)
  }

  private def error[T](
      error: Throwable,
      responseObserver: StreamObserver[T]
  ): Unit = {
    try responseObserver.onError(error)
    catch (withLoggingHandler)
  }

  private val withLoggingHandler: PartialFunction[Throwable, Unit] = { case t: Throwable =>
    log.info(
      s"Failed client callback invocation for connection token ${connectionToken.token}. Got exception $t."
    )
  }

  private def getOldStatus(): Future[intdemo_models.SubjectStatus] = {
    intDemoRepository.findSubjectStatus(connectionToken).map(_.get)
  }

  private def getWalletConnection(): Future[Option[Connection]] = {
    connectorIntegration.getConnectionByToken(connectionToken)
  }

  private def setNextState(
      currentStatus: intdemo_models.SubjectStatus,
      nextStatus: intdemo_models.SubjectStatus
  ): Future[Int] = {
    if (currentStatus != nextStatus)
      intDemoRepository.mergeSubjectStatus(connectionToken, nextStatus)
    else
      Future.successful(0)
  }

  private def isRequiredDataAvailable(maybeRequiredData: Option[D]): Boolean =
    maybeRequiredData.isDefined

  private def isWalletConnected(maybeConnection: Option[Connection]): Boolean =
    maybeConnection.isDefined

  private type Action =
    (Option[D], Option[Connection]) => Future[intdemo_models.SubjectStatus]

  sealed trait State {
    def actionTable: Map[(Boolean, Boolean), Action]

    def apply(
        currentStatus: intdemo_models.SubjectStatus,
        maybeRequiredData: Option[D],
        maybeConnection: Option[Connection]
    ): Future[intdemo_models.SubjectStatus] = {
      val actionTableWithErrors =
        actionTable.withDefaultValue(error(currentStatus))

      actionTableWithErrors(
        (
          isRequiredDataAvailable(maybeRequiredData),
          isWalletConnected(maybeConnection)
        )
      )(
        maybeRequiredData,
        maybeConnection
      )
    }

    def next(status: intdemo_models.SubjectStatus): Action = { (_, _) =>
      Future.successful(status)
    }

    def error(status: intdemo_models.SubjectStatus): Action = { (personalInfo, connection) =>
      Future.failed(
        new IllegalStateException(
          s"Credential issuance service encountered an illegal state where " +
            s"the current status is $status, " +
            s"personal info is $personalInfo and " +
            s"connection state is $connection."
        )
      )
    }

    val emitProofRequest: Action = (_, maybeConnection) => {
      proofRequestIssuer(maybeConnection.get).map { _ =>
        log.info(
          s"Issuer ${issuerId.uuid} proof requests issued. Transitioning to CONNECTED on connection id ${maybeConnection.get.connectionId}."
        )
        intdemo_models.SubjectStatus.CONNECTED
      }
    }

    val emitCredentialAndStop: Action = (maybeRequiredData, maybeConnection) => {
      val requiredData = maybeRequiredData.get
      val connection = maybeConnection.get
      emitCredential(connection.connectionId, requiredData).as(
        intdemo_models.SubjectStatus.CREDENTIAL_SENT
      )
    }

    private def emitCredential(
        connectionId: ConnectionId,
        requiredData: D
    ): Future[intdemo_models.SubjectStatus] = {
      val credential = getCredential(requiredData)
      log.info(
        s"Issuer ${issuerId.uuid} emitting credential to connection with id $connectionId."
      )
      connectorIntegration
        .sendCredential(issuerId, connectionId, credential)
        .as(intdemo_models.SubjectStatus.CREDENTIAL_SENT)
    }
  }

  object State {

    case object UnconnectedState extends State {
      val actionTable = Map(
        // req data, connected...
        (false, false) -> next(intdemo_models.SubjectStatus.UNCONNECTED),
        (false, true) -> emitProofRequest,
        (true, false) -> next(intdemo_models.SubjectStatus.UNCONNECTED),
        (true, true) -> emitCredentialAndStop
      )
    }

    case object ConnectedState extends State {
      val actionTable =
        Map(
          (false, true) -> next(intdemo_models.SubjectStatus.CONNECTED),
          (true, true) -> emitCredentialAndStop
        )
    }

    case object CredentialSentState extends State {
      val actionTable = Map(
        (true, true) -> next(intdemo_models.SubjectStatus.CREDENTIAL_SENT)
      )
    }

    case class IllegalState(status: intdemo_models.SubjectStatus) extends State {
      val actionTable = Map()
    }

    val stateMap = Map[intdemo_models.SubjectStatus, State](
      intdemo_models.SubjectStatus.UNCONNECTED -> UnconnectedState,
      intdemo_models.SubjectStatus.CONNECTED -> ConnectedState,
      intdemo_models.SubjectStatus.CREDENTIAL_SENT -> CredentialSentState
    ).withDefault(IllegalState)
  }
}

object IntDemoStateMachine {
  private val log = LoggerFactory.getLogger(this.getClass)
}
