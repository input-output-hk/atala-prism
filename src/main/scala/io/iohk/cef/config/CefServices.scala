package io.iohk.cef.config
import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{CanValidate, DataItem, DataItemService, TableId}
import io.iohk.cef.ledger.Transaction
import io.iohk.cef.network.NetworkServices
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.transactionservice.NodeTransactionService
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._


private[config] class CefServices(cefConfig: CefConfig) {

  // TODO consider adding logger name and timezone to config
  private val log = LoggerFactory.getLogger("cef")
  private val clock = Clock.systemUTC()

  private val transports = new Transports(cefConfig.peerConfig)

  private val networkDiscovery =
    NetworkServices.networkDiscovery(clock, cefConfig.peerConfig, cefConfig.discoveryConfig)

  def cefTransactionServiceChannel[State, Tx <: Transaction[State]]()(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext): NodeTransactionService[State, Tx] = {
    new TransactionServiceBuilder(cefConfig, log, clock, transports, networkDiscovery).cefTransactionServiceChannel()
  }
  def cefDataItemServiceChannel[T](tableId: TableId, storagePath: Path )(implicit codec:NioCodec[T], typeTag: TypeTag[T] ,canValidate:CanValidate[DataItem[T]]): DataItemService[T] =
    new DataItemServiceBuilder(cefConfig, tableId, storagePath, clock, transports, networkDiscovery)
      .cefDataItemServiceChannel()

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](): AgreementsService[T] =
    new AgreementsServiceBuilder(cefConfig,transports,networkDiscovery)
      .cefAgreementsServiceChannel()

  def shutdown(): Unit =
    transports.shutdown()
}

object CefServices {
  private val services = new ConcurrentHashMap[CefConfig, CefServices]().asScala

  def cefTransactionServiceChannel[State, Tx <: Transaction[State]](cefConfig: CefConfig)(
    implicit stateCodec: NioCodec[State],
    stateTypeTag: TypeTag[State],
    txCodec: NioCodec[Tx],
    txTypeTag: TypeTag[Tx],
    ec: ExecutionContext): NodeTransactionService[State, Tx] = {
    services.getOrElseUpdate(cefConfig, new CefServices(cefConfig)).cefTransactionServiceChannel()
  }
  def cefDataItemServiceChannel[T: NioCodec: TypeTag](
                                                       cefConfig: CefConfig,
                                                       tableId: TableId,
                                                       storagePath: Path)(implicit canValidate:CanValidate[DataItem[T]]): DataItemService[T] =
    services.getOrElseUpdate(cefConfig, new CefServices(cefConfig)).cefDataItemServiceChannel(tableId,storagePath)

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](
                                                         cefConfig: CefConfig): AgreementsService[T] =
    services.getOrElseUpdate(cefConfig, new CefServices(cefConfig)).cefAgreementsServiceChannel()

  def shutdown: Unit = {
    services.map(_._2).foreach(_.shutdown())
  }
}