package io.iohk.cef.frontend.client

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.iohk.cef.frontend.DefaultJsonFormats
import io.iohk.cef.frontend.client.IdentityClientActor._
import io.swagger.annotations._
import javax.ws.rs.Path
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Api(value = "transaction", produces = "application/json")
@Path("/transaction")
class IdentityServiceApi(createActor: ActorRef)(implicit executionContext: ExecutionContext)
    extends Directives
    with DefaultJsonFormats {

  implicit val timeout = Timeout(2.seconds)

  val route = createTransaction
  @Path("/create")
  @ApiOperation(value = "Transaction Request", nickname = "Message Request", httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        value = "Message Request",
        required = true,
        dataTypeClass = classOf[TransactionRequest],
        paramType = "body")
    ))
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "Created"),
      new ApiResponse(code = 500, message = "Internal server error")
    ))
  def createTransaction: Route =
    path("transaction" / "create") {
      post {
        entity(as[TransactionRequest]) { request =>
          val responseHandler: Future[TransactionResponse] =
            (createActor ? request).mapTo[TransactionResponse]
          onSuccess(responseHandler) { response =>
            complete(
              response.result match {
                case Right(_) => StatusCodes.Created
                case Left(_) => StatusCodes.Conflict
              }
            )
          }
        }
      }
    }
}
