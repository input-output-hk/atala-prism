package io.iohk.cef.config

import java.nio.file.Path
import java.time.Clock
import java.util.UUID

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.{QueryEngine, QueryRequest, QueryResponse}
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.cef.network.Network
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.transactionservice.Envelope

import scala.reflect.runtime.universe._

private[config] class DataItemServiceBuilder(
    cefConfig: CefConfig,
    tableId: TableId,
    storagePath:Path,
    clock: Clock,
    transports: Transports,
    networkDiscovery: NetworkDiscovery) {

  def cefDataItemServiceChannel[T]()(implicit codec:NioCodec[T], typeTag: TypeTag[T] ,canValidate:CanValidate[DataItem[T]]): DataItemService[T] = {

    val tableStorage = new MVTableStorage[T](tableId,storagePath)
    val table = new Table(tableId,tableStorage)
    val network = Network[Envelope[DataItemAction[T]]](networkDiscovery, transports)
    val requestNetwork = Network[Envelope[QueryRequest]](networkDiscovery, transports)
    val responseNetwork = Network[Envelope[QueryResponse[T]]](networkDiscovery, transports)

    val queryEngine =
      new QueryEngine(cefConfig.peerConfig.nodeId, table, requestNetwork, responseNetwork, () => UUID.randomUUID().toString)

    new DataItemService[T](table, network,queryEngine)
  }
}
