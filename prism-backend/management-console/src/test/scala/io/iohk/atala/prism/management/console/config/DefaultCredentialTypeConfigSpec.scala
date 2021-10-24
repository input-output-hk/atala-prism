package io.iohk.atala.prism.management.console.config

import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.credentials.utils.Mustache
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.interop.KotlinFunctionConverters._

import scala.util.Try

// Check correctness of each credential type defined in DefaultCredentialTypes/default-credential-types.conf
// sbt "project management-console" "testOnly *DefaultCredentialTypeConfigSpec"
class DefaultCredentialTypeConfigSpec extends AnyWordSpec with Matchers {

  "defaultCredentialTypes" should {
    "be valid mustache templates" in {
      DefaultCredentialTypeConfig(ConfigFactory.load()).defaultCredentialTypes
        .foreach { defaultCredentialType =>
          val res: Either[Throwable, String] = Try(
            Mustache.INSTANCE.render(
              defaultCredentialType.template,
              (
                  (name: String) =>
                    defaultCredentialType.fields
                      .find(_.name == name)
                      .map(_.name)
                      .orNull
              ).asKotlin,
              true
            )
          ).toEither
          res mustBe a[Right[_, _]]
        }
    }

    "have unique names within institution" in {
      val names = DefaultCredentialTypeConfig(
        ConfigFactory.load()
      ).defaultCredentialTypes.map(_.name)
      names.size mustBe names.distinct.size
    }

    "all have fields with unique name within user defined credential type" in {
      DefaultCredentialTypeConfig(ConfigFactory.load()).defaultCredentialTypes
        .foreach { defaultCredentialType =>
          val fieldNames = defaultCredentialType.fields.map(_.name)
          fieldNames.size mustBe fieldNames.distinct.size
        }
    }
  }

}
