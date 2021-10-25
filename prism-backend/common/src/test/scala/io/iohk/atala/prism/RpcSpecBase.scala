package io.iohk.atala.prism

import cats.effect.IO
import io.grpc.{CallCredentials, CallOptions, ManagedChannel, Metadata, Server, ServerServiceDefinition}
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, GrpcAuthenticatorInterceptor, SignedRequestsHelper}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import org.scalatest.BeforeAndAfterEach
import scalapb.GeneratedMessage

import _root_.java.util.concurrent.{Executor, TimeUnit}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import tofu.logging.Logs

trait ApiTestHelper[STUB] {
  def apply[T](
      requestNonce: Vector[Byte],
      signature: ECSignature,
      publicKey: ECPublicKey,
      traceId: TraceId
  )(
      f: STUB => T
  ): T
  def apply[T](
      requestNonce: Vector[Byte],
      signature: ECSignature,
      did: DID,
      keyId: String,
      traceId: TraceId
  )(
      f: STUB => T
  ): T
  def apply[T](
      requestNonce: Vector[Byte],
      keys: ECKeyPair,
      request: GeneratedMessage
  )(f: STUB => T): T = {
    val payload = SignedRequestsHelper
      .merge(auth.model.RequestNonce(requestNonce), request.toByteArray)
      .toArray
    val signature = EC.signBytes(payload.array, keys.getPrivateKey)
    apply(requestNonce, signature, keys.getPublicKey, TraceId.generateYOLO)(f)
  }
  def apply[T, R <: GeneratedMessage](
      rpcRequest: SignedRpcRequest[R]
  )(f: STUB => T): T =
    apply(
      rpcRequest.nonce,
      rpcRequest.signature,
      rpcRequest.did,
      rpcRequest.keyId,
      TraceId.generateYOLO
    )(f)
  def unlogged[T](f: STUB => T): T
}

abstract class RpcSpecBase extends AtalaWithPostgresSpec with BeforeAndAfterEach {

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _

  val testLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  def services: Seq[ServerServiceDefinition]

  override def beforeEach(): Unit = {
    super.beforeEach()

    serverName = InProcessServerBuilder.generateName()

    val serverBuilderWithoutServices = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .intercept(new GrpcAuthenticatorInterceptor)

    val serverBuilder = services.foldLeft(serverBuilderWithoutServices) { (builder, service) =>
      builder.addService(service)
    }

    serverHandle = serverBuilder.build().start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()
  }

  override def afterEach(): Unit = {
    // Gracefully shut down the channel with a 10s deadline and then force it to ensure it's released
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    channelHandle.shutdownNow()
    // Gracefully shut down the server with a 10s deadline and then force it to ensure it's released
    serverHandle.shutdown()
    serverHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdownNow()

    super.afterEach()
  }

  def usingApiAsConstructor[STUB](
      stubFactory: (ManagedChannel, CallOptions) => STUB
  ): ApiTestHelper[STUB] =
    new ApiTestHelper[STUB] {
      override def unlogged[T](f: STUB => T): T = {
        val blockingStub = stubFactory(channelHandle, CallOptions.DEFAULT)
        f(blockingStub)
      }

      private def apply[T](metadata: Metadata)(f: STUB => T): T = {
        val callOptions =
          CallOptions.DEFAULT.withCallCredentials(new CallCredentials {
            override def applyRequestMetadata(
                requestInfo: CallCredentials.RequestInfo,
                appExecutor: Executor,
                applier: CallCredentials.MetadataApplier
            ): Unit = {
              appExecutor.execute { () =>
                applier.apply(metadata)
              }
            }

            override def thisUsesUnstableApi(): Unit = ()
          })

        val blockingStub = stubFactory(channelHandle, callOptions)
        f(blockingStub)
      }

      override def apply[T](
          requestNonce: Vector[Byte],
          signature: ECSignature,
          publicKey: ECPublicKey,
          traceId: TraceId
      )(
          f: STUB => T
      ): T = {
        apply(
          GrpcAuthenticationHeader
            .PublicKeyBased(
              auth.model.RequestNonce(requestNonce),
              publicKey,
              signature
            )
            .toMetadata
        )(f)
      }

      override def apply[T](
          requestNonce: Vector[Byte],
          signature: ECSignature,
          did: DID,
          keyId: String,
          traceId: TraceId
      )(
          f: STUB => T
      ): T = {
        apply(
          GrpcAuthenticationHeader
            .PublishedDIDBased(
              auth.model.RequestNonce(requestNonce),
              did,
              keyId,
              signature
            )
            .toMetadata
        )(f)
      }
    }
}
