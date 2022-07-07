package io.iohk.atala.prism.auth.grpc

import com.typesafe.config.Config
import io.grpc._
import io.iohk.atala.prism.auth.model.AuthToken
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters._

class AuthorizationInterceptor(globalConfig: Config) extends ServerInterceptor {
  import GrpcAuthenticationContext._
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val authEnabled: Boolean = globalConfig.getBoolean("api.authEnabled")
  private val authTokens: Set[AuthToken] = loadAuthTokens(globalConfig: Config)

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    if (authEnabled) {
      val mayBeAuthToken = getAuthTokenFromMetadata(headers)
      if (mayBeAuthToken.exists(authTokens.contains)) {
        Contexts.interceptCall(Context.current(), call, headers, next)
      } else {
        call.close(
          io.grpc.Status.UNAUTHENTICATED.withDescription(
            "The 'prism-auth-token' is missing from headers / The provided `prism-auth-token` is invalid"
          ),
          headers
        )
        new ServerCall.Listener[ReqT] {} // noop
      }
    } else {
      Contexts.interceptCall(Context.current(), call, headers, next)
    }
  }

  private def loadAuthTokens(globalConfig: Config): Set[AuthToken] = {
    logger.info("Loading AuthTokens")
    val authConfig = globalConfig.getConfig("api")
    logger.info(s"AuthTokens enable $authEnabled")
    if (authEnabled) {
      authConfig.getStringList("authTokens").asScala.map(AuthToken).toSet
    } else Set.empty[AuthToken]
  }
}
