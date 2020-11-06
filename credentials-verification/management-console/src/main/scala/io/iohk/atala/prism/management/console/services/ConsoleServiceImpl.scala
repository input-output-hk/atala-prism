package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api._
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

class ConsoleServiceImpl(@nowarn contactsRepository: ContactsRepository)(implicit
    @nowarn ec: ExecutionContext
) extends ConsoleServiceGrpc.ConsoleService {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] = ???

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] = ???

  override def getContact(request: GetContactRequest): Future[GetContactResponse] = ???

  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = ???

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] = ???
}
