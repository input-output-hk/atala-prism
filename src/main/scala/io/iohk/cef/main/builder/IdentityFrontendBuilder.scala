package io.iohk.cef.main.builder
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.IdentitiesController
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.{ExecutionContext, Future}

trait IdentityFrontendBuilder {
  self: ActorSystemBuilder
    with IdentityTransactionServiceBuilder
    with CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]
    with ConfigReaderBuilder =>

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()

  def bindingFuture(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]],
      ebByteStringSerializable: ByteStringSerializable[IdentityTransaction],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Future[Http.ServerBinding] = {

    val serviceApi = new IdentitiesController(service)
    val route = serviceApi.routes
    Http()(actorSystem)
      .bindAndHandle(route, config.getString("frontend.rest.interface"), config.getInt("frontend.rest.port"))
  }
}
