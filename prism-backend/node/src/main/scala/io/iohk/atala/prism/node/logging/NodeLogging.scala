package io.iohk.atala.prism.node.logging

import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.Logger
import scalapb.GeneratedMessage

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object NodeLogging {

  def withLog[Response <: GeneratedMessage, Req <: GeneratedMessage](methodName: String, request: Req)(
      code: String => Future[Response]
  )(implicit ec: ExecutionContext, logger: Logger): Future[Response] = {
    val traceId = UUID.randomUUID().toString
    logger.info(
      s"methodName:$methodName traceId = $traceId request = ${request.toProtoString}",
      kv("methodName", methodName),
      kv("traceId", traceId)
    )
    try { code(traceId).map(resp => logAndReturnResponse(methodName, traceId, resp)) }
    catch {
      case NonFatal(ex) =>
        logger.error(
          s"methodName:$methodName Error: Non Fatal Error traceId = $traceId \n Exception : $ex",
          kv("methodName", methodName),
          kv("traceId", traceId)
        )
        throw new RuntimeException(ex)
    }
  }

  def logWithTraceId[Req <: GeneratedMessage](methodName: String, traceId: String, argsToLog: (String, String)*)(
      implicit logger: Logger
  ): Unit = {
    logger.info(
      s"methodName:$methodName traceId = $traceId, \n  ${argsToLog.map(x => s"${x._1}=${x._2}").mkString(",")}",
      kv("methodName", methodName),
      kv("traceId", traceId)
    )
  }

  def logAndReturnResponse[Response <: GeneratedMessage](methodName: String, traceId: String, response: Response)(
      implicit logger: Logger
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
