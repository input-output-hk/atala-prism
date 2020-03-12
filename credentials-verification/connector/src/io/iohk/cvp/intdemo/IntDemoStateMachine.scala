package io.iohk.cvp.intdemo

import credential.Credential
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, ConnectionId, TokenString}
import io.iohk.cvp.intdemo.IntDemoStateMachine.log
import io.iohk.cvp.intdemo.protos.{GetSubjectStatusResponse, SubjectStatus}
import io.iohk.cvp.intdemo.protos.SubjectStatus.{CONNECTED, CREDENTIAL_SENT, UNCONNECTED}
import io.iohk.cvp.models.ParticipantId
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class IntDemoStateMachine[D](
    requiredDataLoader: TokenString => Future[Option[D]],
    getCredential: D => Credential,
    connectorIntegration: ConnectorIntegration,
    intDemoRepository: IntDemoRepository,
    connectionToken: TokenString,
    issuerId: ParticipantId,
    responseObserver: StreamObserver[GetSubjectStatusResponse],
    scheduler: Scheduler,
    schedulerPeriod: FiniteDuration
)(implicit ec: ExecutionContext) {

  def tick(): Unit = {
    val f = for {
      currentStatus <- getCurrentStatus()
      requiredData <- requiredDataLoader(connectionToken)
      connection <- getWalletConnection()
      nextStatus <- State.stateMap(currentStatus)(currentStatus, requiredData, connection)
      _ <- setNextState(currentStatus, nextStatus)
    } yield nextStatus

    f.onComplete {
      case Success(status) =>
        if (status == CREDENTIAL_SENT) {
          complete(GetSubjectStatusResponse(status), responseObserver)
        } else {
          next(GetSubjectStatusResponse(status), responseObserver)
        }
      case Failure(exception) =>
        error(exception, responseObserver)
    }
  }

  private def next[T](response: T, responseObserver: StreamObserver[T]): Unit = {
    try {
      responseObserver.onNext(response)
      scheduler.scheduleOnce(schedulerPeriod)(tick())
    } catch (withLoggingHandler)
  }

  private def complete[T](response: T, responseObserver: StreamObserver[T]): Unit = {
    try {
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (withLoggingHandler)
  }

  private def error[T](error: Throwable, responseObserver: StreamObserver[T]): Unit = {
    try responseObserver.onError(error)
    catch (withLoggingHandler)
  }

  private val withLoggingHandler: PartialFunction[Throwable, Unit] = {
    case t: Throwable =>
      log.info(s"Failed client callback invocation for connection token ${connectionToken.token}. Got exception $t.")
  }

  private def getCurrentStatus(): Future[SubjectStatus] = {
    intDemoRepository.findSubjectStatus(connectionToken).map(_.get)
  }

  private def getWalletConnection(): Future[Option[Connection]] = {
    connectorIntegration.getConnectionByToken(connectionToken)
  }

  private def setNextState(currentStatus: SubjectStatus, nextStatus: SubjectStatus): Future[Int] = {
    if (currentStatus != nextStatus)
      intDemoRepository.mergeSubjectStatus(connectionToken, nextStatus)
    else
      Future.successful(0)
  }

  private def isRequiredDataAvailable(maybeRequiredData: Option[D]): Boolean =
    maybeRequiredData.isDefined

  private def isWalletConnected(maybeConnection: Option[Connection]): Boolean =
    maybeConnection.isDefined

  private type Action = (Option[D], Option[Connection]) => Future[SubjectStatus]

  sealed trait State {
    def actionTable: Map[(Boolean, Boolean), Action]

    def apply(
        currentStatus: SubjectStatus,
        maybeRequiredData: Option[D],
        maybeConnection: Option[Connection]
    ): Future[SubjectStatus] = {
      val actionTableWithErrors = actionTable.withDefaultValue(error(currentStatus))

      actionTableWithErrors(isRequiredDataAvailable(maybeRequiredData), isWalletConnected(maybeConnection))(
        maybeRequiredData,
        maybeConnection
      )
    }

    def next(status: SubjectStatus): Action = { (_, _) =>
      Future.successful(status)
    }

    def error(status: SubjectStatus): Action = { (personalInfo, connection) =>
      Future.failed(
        new IllegalStateException(
          s"Credential issuance service encountered an illegal state where " +
            s"the current status is $status, " +
            s"personal info is $personalInfo and " +
            s"connection state is $connection."
        )
      )
    }

    val emitCredentialAndStop: Action = (maybeRequiredData, maybeConnection) => {
      val requiredData = maybeRequiredData.get
      val connection = maybeConnection.get
      emitCredential(connection.connectionId, requiredData).map(_ => CREDENTIAL_SENT)
    }

    private def emitCredential(connectionId: ConnectionId, requiredData: D): Future[SubjectStatus] = {
      val credential = getCredential(requiredData)
      log.info(s"Issuer ${issuerId.uuid} emitting credential to connection with ID $connectionId.")
      connectorIntegration.sendCredential(issuerId, connectionId, credential).map(_ => CREDENTIAL_SENT)
    }
  }

  object State {

    case object UnconnectedState extends State {
      val actionTable = Map(
        (false, false) -> next(UNCONNECTED),
        (false, true) -> next(CONNECTED),
        (true, true) -> emitCredentialAndStop,
        (true, false) -> next(UNCONNECTED)
      )
    }

    case object ConnectedState extends State {
      val actionTable = Map((false, true) -> next(CONNECTED), (true, true) -> emitCredentialAndStop)
    }

    case object CredentialSentState extends State {
      val actionTable = Map(
        (true, true) -> next(CREDENTIAL_SENT)
      )
    }

    case class IllegalState(status: SubjectStatus) extends State {
      val actionTable = Map()
    }

    val stateMap = Map[SubjectStatus, State](
      UNCONNECTED -> UnconnectedState,
      CONNECTED -> ConnectedState,
      CREDENTIAL_SENT -> CredentialSentState
    ).withDefault(IllegalState)
  }
}

object IntDemoStateMachine {
  private val log = LoggerFactory.getLogger(this.getClass)
}
