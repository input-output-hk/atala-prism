package io.iohk.atala.prism.management.console.config

import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.kotlin.credentials.utils.Mustache
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

// Check correctness of each credential type defined in DefaultCredentialTypes/default-credential-types.conf
// sbt "project management-console" "testOnly *DefaultCredentialTypeConfigSpec"
class DefaultCredentialTypeConfigSpec extends AnyWordSpec with Matchers {

  "defaultCredentialTypes" should {
    "be valid mustache templates" in {
      DefaultCredentialTypeConfig(ConfigFactory.load()).defaultCredentialTypes.foreach { defaultCredentialType =>
        Mustache.INSTANCE.render(
          defaultCredentialType.template,
          name => defaultCredentialType.fields.find(_.name == name).map(_.name)
        ) mustBe a[Right[_, _]]
      }
    }

    "have unique names within institution" in {
      val names = DefaultCredentialTypeConfig(ConfigFactory.load()).defaultCredentialTypes.map(_.name)
      names.size mustBe names.distinct.size
    }

    "all have fields with unique name within user defined credential type" in {
      DefaultCredentialTypeConfig(ConfigFactory.load()).defaultCredentialTypes.foreach { defaultCredentialType =>
        val fieldNames = defaultCredentialType.fields.map(_.name)
        fieldNames.size mustBe fieldNames.distinct.size
      }
    }
  }

}
