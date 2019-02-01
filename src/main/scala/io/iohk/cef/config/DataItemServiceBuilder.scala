package io.iohk.cef.config

import java.nio.file.Path
import java.util.UUID

import io.iohk.codecs.nio._
import io.iohk.codecs.nio.auto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.{DataItemQueryEngine, DataItemQueryRequest, DataItemQueryResponse}
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.network.{Envelope, Network, NetworkConfig}

import scala.reflect.runtime.universe._

private[config] class DataItemServiceBuilder(
    networkConfig: NetworkConfig,
    tableId: TableId,
    storagePath: Path
) {

  def cefDataItemServiceChannel[T]()(
      implicit codec: NioCodec[T],
      typeTag: TypeTag[T],
      canValidate: CanValidate[DataItem[T]]
  ): DataItemService[T] = {

    val tableStorage = new MVTableStorage[T](tableId, storagePath)
    val table = new Table(tableId, tableStorage)
    val network = Network[Envelope[DataItemAction[T]]](networkConfig.discovery, networkConfig.transports)
    val requestNetwork = Network[Envelope[DataItemQueryRequest]](networkConfig.discovery, networkConfig.transports)
    val responseNetwork = Network[Envelope[DataItemQueryResponse[T]]](networkConfig.discovery, networkConfig.transports)

    val queryEngine =
      new DataItemQueryEngine(
        networkConfig.peerConfig.nodeId,
        table,
        requestNetwork,
        responseNetwork,
        () => UUID.randomUUID().toString
      )

    new DataItemService[T](table, network, queryEngine)
  }
}
