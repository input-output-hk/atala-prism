package io.iohk.connector

import io.grpc.{Server, ServerBuilder, Status}
import io.iohk.connector.protos._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Run with `mill -i connector.run`, otherwise, the server will stay running even after ctrl+C.
  */
object ServerApp {
  def main(args: Array[String]): Unit = {
    val server = new ServerApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ServerApp(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(ServerApp.port)
      .addService(ConnectorServiceGrpc.bindService(new ConnectorServiceImpl, executionContext))
      .build()
      .start()

    println("Server started, listening on " + ServerApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class ConnectorServiceImpl extends ConnectorServiceGrpc.ConnectorService {
    /** Get active connections for current participant
      *
      * Available to: Holder, Issuer, Validator
      */
    override def getConnectionsSince(request: GetConnectionsSinceRequest): Future[GetConnectionsSinceResponse] = {
      Future.successful {
        GetConnectionsSinceResponse(Nil)
      }
    }

    /** Return info about connection token such as creator info
      *
      * Available to: Holder
      *
      * Errors:
      * Token does not exist (UNKNOWN)
      */
    override def getConnectionTokenInfo(request: GetConnectionTokenInfoRequest): Future[GetConnectionTokenInfoResponse] = {
      Future.successful {
        GetConnectionTokenInfoResponse(
          ParticipantInfo(ParticipantInfo.Participant.Issuer(
            IssuerInfo()
          ))
        )
      }
    }

    /** Instantiate connection from connection token
      *
      * Available to: Holder
      *
      * Errors:
      * Token does not exist (UNKNOWN)
      */
    override def addConnectionFromToken(request: AddConnectionFromTokenRequest): Future[AddConnectionFromTokenResponse] = {
      Future.successful {
        AddConnectionFromTokenResponse()
      }
    }

    /** Delete active connection
      *
      * Available to: Holder, Issuer, Validator
      *
      * Errors:
      * Connection does not exist (UNKNOWN)
      */
    override def deleteConnection(request: DeleteConnectionRequest): Future[DeleteConnectionResponse] = {
      Future.successful {
        DeleteConnectionResponse()
      }
    }

    /** Bind DID to issuer
      *
      * Available to: Issuer
      *
      * Errors:
      * Invalid DID (INVALID_ARGUMENT)
      * Invalid DID document (INVALID_ARGUMENT)
      * DID Document does not match DID (INVALID_ARGUMENT)
      */
    override def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
      Future.successful {
        RegisterDIDResponse()
      }
    }

    /** Change billing plan of participant who wants to generate connection tokens
      *
      * Available to: Issuer, Validator
      *
      * Errors:
      * Unknown billing plan (UNKNOWN)
      * User not allowed to set this billing plan (PERMISSION_DENIED)
      */
    override def changeBillingPlan(request: ChangeBillingPlanRequest): Future[ChangeBillingPlanResponse] = {
      Future.successful {
        ChangeBillingPlanResponse()
      }
    }

    /** Generate connection token that can be used to instantiate connection
      *
      * Available to: Issuer, Validator
      *
      * Errors:
      * Billing plan doesn't allow token generation (PERMISSION_DENIED)
      */
    override def generateConnectionToken(request: GenerateConnectionTokenRequest): Future[GenerateConnectionTokenResponse] = {
      Future.failed {
        Status.PERMISSION_DENIED.withDescription("Billing plan doesn't allow token generation").asException()
      }
    }

    /** Return messages received after given time moment, sorted in ascending order by receive time
      *
      * Available to: Issuer, Holder, Validator
      */
    override def getMessagesSince(request: GetMessagesSinceRequest): Future[GetMessagesSinceResponse] = {
      Future.successful {
        GetMessagesSinceResponse(Nil)
      }
    }

    /** Send message over a connection
      *
      * Available to: Issuer, Holder, Validator
      *
      * Errors:
      * Unknown connection (UNKNOWN)
      * Connection closed (FAILED_PRECONDITION)
      */
    override def sendMessage(request: SendMessageRequest): Future[SendMessageResponse] = {
      Future.successful {
        SendMessageResponse()
      }
    }
  }
}
