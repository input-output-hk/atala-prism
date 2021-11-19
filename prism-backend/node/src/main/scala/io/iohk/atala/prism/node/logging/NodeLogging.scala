package io.iohk.atala.prism.node.logging

import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.protos.node_internal.AtalaObject
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.Logger
import scalapb.GeneratedMessage
import io.iohk.atala.prism.tracing.Tracing._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object NodeLogging {

  def withLog[Response <: GeneratedMessage, Req <: GeneratedMessage](
      methodName: String,
      request: Req
  )(
      code: TraceId => Future[Response]
  )(implicit ec: ExecutionContext, logger: Logger): Future[Response] = {
    trace { traceId =>
      logger.info(
        s"methodName:$methodName traceId = $traceId request = ${request.toProtoString}",
        kv("methodName", methodName),
        kv("traceId", traceId)
      )
      try {
        code(traceId).map(resp => logAndReturnResponse(methodName, traceId, resp))
      } catch {
        case NonFatal(ex) =>
          logger.error(
            s"methodName:$methodName Error: Non Fatal Error traceId = $traceId \n Exception : $ex",
            kv("methodName", methodName),
            kv("traceId", traceId)
          )
          throw new RuntimeException(ex)
      }
    }
  }

  def logWithTraceId(
      methodName: String,
      traceId: TraceId,
      argsToLog: (String, String)*
  )(implicit
      logger: Logger
  ): Unit = {
    logger.info(
      s"methodName:$methodName traceId = $traceId, \n  ${argsToLog
        .map(x => s"${x._1}=${x._2}")
        .mkString(",")}",
      kv("methodName", methodName),
      kv("traceId", traceId)
    )
  }

  def logOperationIds(methodName: String, message: String, obj: AtalaObject)(implicit
      logger: Logger
  ): Unit = {
    obj.blockContent.foreach { block =>
      val atalaOperationIds = block.operations.toList.map(AtalaOperationId.of)
      logger.error(
        s"methodName:$methodName , message: $message, AtalaObjectId : ${obj.toString},\n atalaOperationIds = [${atalaOperationIds
          .mkString(",")}]"
      )
    }
  }

  def logAndReturnResponse[Response <: GeneratedMessage](
      methodName: String,
      traceId: TraceId,
      response: Response
  )(implicit
      logger: Logger
  ): Response = {
    ("traceId" -> traceId)
    logger.info(
      s"$methodName \n traceId = $traceId \n response = ${response.toProtoString}",
      kv("methodName", methodName),
      kv("traceId", traceId)
    )
    response
  }

}
