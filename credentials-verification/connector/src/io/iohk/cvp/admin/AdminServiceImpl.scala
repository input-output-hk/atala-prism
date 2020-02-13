package io.iohk.cvp.admin

import io.iohk.cvp.admin.Errors.AdminError.DatabaseError
import io.iohk.cvp.admin.protos.AdminServiceGrpc.AdminService
import io.iohk.cvp.admin.protos.{PopulateDemoDatasetRequest, PopulateDemoDatasetResponse}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class AdminServiceImpl(repository: AdminRepository)(implicit ec: ExecutionContext) extends AdminService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def populateDemoDataset(request: PopulateDemoDatasetRequest): Future[PopulateDemoDatasetResponse] = {
    logger.info("Populating the database with the demo dataset.")
    for {
      result: Either[DatabaseError, List[Int]] <- repository.insertDemoDataset().value
    } yield {
      result match {
        case Left(error) =>
          val errorDesc = error.toStatus.getDescription
          logger.info(s"Population of demo dataset failed with message: $errorDesc.")
          PopulateDemoDatasetResponse(error.toStatus.getDescription)
        case Right(rows) =>
          val rowsUpdated = rows.sum
          logger.warn(s"Population of demo dataset complete. $rowsUpdated rows updated.")
          PopulateDemoDatasetResponse(s"$rowsUpdated rows updated")
      }
    }
  }
}
