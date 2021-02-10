package io.iohk.atala.prism.management.console.config

import com.typesafe.config.Config
import io.iohk.atala.prism.management.console.models.CreateCredentialTypeField

import scala.io.Source
import scala.jdk.CollectionConverters._

case class DefaultCredentialType(name: String, template: String, fields: List[CreateCredentialTypeField])

object DefaultCredentialType {
  def apply(config: Config): DefaultCredentialType = {
    DefaultCredentialType(
      name = config.getString("name"),
      template = readFileFromResource(config.getString("fileName")),
      fields = config.getConfigList("fields").asScala.toList.map { config =>
        CreateCredentialTypeField(
          name = config.getString("name"),
          description = config.getString("description")
        )
      }
    )
  }

  private def readFileFromResource(fileName: String) =
    Source.fromResource(s"DefaultCredentialTypes/$fileName").getLines().mkString("\n")
}

case class DefaultCredentialTypeConfig(defaultCredentialTypes: List[DefaultCredentialType])

object DefaultCredentialTypeConfig {

  def apply(globalConfig: Config): DefaultCredentialTypeConfig = {
    DefaultCredentialTypeConfig(
      globalConfig.getConfigList("defaultCredentialTypes").asScala.toList.map(config => DefaultCredentialType(config))
    )
  }
}
