package io.iohk.cef.frontend.client

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import io.iohk.cef.frontend.DefaultJsonFormats
import io.iohk.cef.frontend.client.TransactionClient._
import io.swagger.annotations._
import javax.ws.rs.Path

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Api(value = "transaction", produces = "application/json")
@Path("/transaction")
class IdentityServiceApi(createActor: ActorRef)(implicit executionContext: ExecutionContext)
    extends Directives
    with DefaultJsonFormats {

  implicit val timeout = Timeout(2.seconds)

  val route = createIdentity
  @Path("/identity")
  @ApiOperation(value = "Transaction Request", nickname = "Message Request", httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        value = "Message Request",
        required = true,
        dataTypeClass = classOf[IdentityTransactionRequest],
        paramType = "body")
    ))
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "Created"),
      new ApiResponse(code = 400, message = "BadRequest Malformed Json")
    ))
  def createIdentity: Route =
    path("transaction" / "identity") {
      post {
        entity(as[IdentityTransactionRequest]) { request =>
          val responseHandler: Future[TransactionResponse] =
            (createActor ? request).mapTo[TransactionResponse]
          onSuccess(responseHandler) { response =>
            complete(
              response.result match {
                case Right(_) => {
                  println(response.result)
                  StatusCodes.Created
                }
                case Left(_) => {
                  println(response.result)
                  StatusCodes.BadRequest
                }
              }
            )
          }
        }
      }
    }
}
