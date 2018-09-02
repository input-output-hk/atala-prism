package io.iohk.cef.frontend

import akka.actor.ActorSystem
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.typesafe.config.ConfigFactory
import io.iohk.cef.frontend.client.IdentityServiceApi
import io.swagger.models.Scheme
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.models.auth.In.QUERY

case class Swagger(system: ActorSystem) extends SwaggerHttpService {
  val config = ConfigFactory.load()
  val API_URL = config.getString("swagger.api.url")
  val BASE_PATH = config.getString("swagger.api.base.path")
  val PROTOCOL = config.getString("swagger.api.protocol")

  override def apiClasses: Set[Class[_]] = Set(classOf[IdentityServiceApi])

  override val host = API_URL

  override val basePath = BASE_PATH

  override def schemes: List[Scheme] = List(Scheme.forValue(PROTOCOL))

  override def apiDocsPath: String = "api-docs"

  val apiKey = new ApiKeyAuthDefinition("api_key", QUERY)

  override val securitySchemeDefinitions: Map[String, ApiKeyAuthDefinition] = Map("apiKey" -> apiKey)

  override def info: Info =
    new Info("Swagger Akka HTTP Demo Application...", "1.0", "Swagger API", "", None, None, Map.empty)
}
