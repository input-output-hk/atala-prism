package io.iohk.cef.config

import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.data.{CanValidate, DataItem, DataItemService, TableId}
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.storage.{LedgerStateStorage, LedgerStorage}
import io.iohk.cef.ledger.{AppliedBlocksSubject, Transaction}
import io.iohk.cef.transactionservice.NodeTransactionService
import io.iohk.codecs.nio._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

private[config] class CefServices(cefConfig: CefConfig) {

  // TODO consider adding logger name and timezone to config
  private val log = LoggerFactory.getLogger("cef")
  private val clock = Clock.systemUTC()

  def cefTransactionServiceChannel[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx],
      queryService: LedgerQueryService[State, Q],
      appliedBlocksSubject: AppliedBlocksSubject[State, Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): NodeTransactionService[State, Tx, Q] = {

    new TransactionServiceBuilder(cefConfig, log, clock)
      .cefTransactionServiceChannel(ledgerStateStorage, ledgerStorage, queryService, appliedBlocksSubject)
  }

  def cefDataItemServiceChannel[T](
      tableId: TableId,
      storagePath: Path
  )(implicit codec: NioCodec[T], typeTag: TypeTag[T], canValidate: CanValidate[DataItem[T]]): DataItemService[T] = {

    new DataItemServiceBuilder(cefConfig.networkConfig, tableId, storagePath)
      .cefDataItemServiceChannel()
  }

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](): AgreementsService[T] = {
    new AgreementsServiceBuilder(cefConfig.networkConfig)
      .cefAgreementsServiceChannel()
  }

  def shutdown(): Unit = {
    cefConfig.networkConfig.transports.shutdown()
  }
}

object CefServices {

  private val services = new ConcurrentHashMap[CefConfig, CefServices]().asScala

  def cefTransactionServiceChannel[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      cefConfig: CefConfig,
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx],
      queryService: LedgerQueryService[State, Q],
      appliedBlocksSubject: AppliedBlocksSubject[State, Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): NodeTransactionService[State, Tx, Q] = {

    services
      .getOrElseUpdate(cefConfig, new CefServices(cefConfig))
      .cefTransactionServiceChannel(ledgerStateStorage, ledgerStorage, queryService, appliedBlocksSubject)
  }

  def cefDataItemServiceChannel[T: NioCodec: TypeTag](
      cefConfig: CefConfig,
      tableId: TableId,
      storagePath: Path
  )(implicit canValidate: CanValidate[DataItem[T]]): DataItemService[T] = {

    services
      .getOrElseUpdate(cefConfig, new CefServices(cefConfig))
      .cefDataItemServiceChannel(tableId, storagePath)
  }

  def cefAgreementsServiceChannel[T: NioCodec: TypeTag](cefConfig: CefConfig): AgreementsService[T] = {

    services
      .getOrElseUpdate(cefConfig, new CefServices(cefConfig))
      .cefAgreementsServiceChannel()
  }

  def shutdown(): Unit = {
    services.foreach { case (_, service) => service.shutdown() }
  }
}
