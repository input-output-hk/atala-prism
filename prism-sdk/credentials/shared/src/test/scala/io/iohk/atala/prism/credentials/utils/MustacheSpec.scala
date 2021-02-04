package io.iohk.atala.prism.credentials.utils

import cats.data.{NonEmptyList, Validated}
import scala.util.parsing.combinator.JavaTokenParsers

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.credentials.utils.Mustache._

class MustacheSpec extends AnyWordSpec with Matchers with EitherValues with JavaTokenParsers {

  "Mustache" should {
    "parse literals" in new Fixtures {
      // invalid
      mustache.parse(mustache.literal, "") mustBe a[Failure]
      mustache.parse(mustache.literal, "}}") mustBe a[Failure]
      mustache.parse(mustache.literal, "{{") mustBe a[Failure]

      // valid
      mustache.parse(mustache.literal, "<div>any content</div>").get mustBe Literal("<div>any content</div>")
      mustache.parse(mustache.literal, singlelineWithoutTags).get mustBe Literal(singlelineWithoutTags)
      mustache.parse(mustache.literal, multilineWithoutTags).get mustBe Literal(multilineWithoutTags)
    }

    "parse variable" in new Fixtures {
      // invalid
      mustache.parse(mustache.variable, "") mustBe a[Failure]

      // valid
      mustache.parse(mustache.variable, "{{ variable }}").get mustBe Variable("variable", escape = true)
      mustache.parse(mustache.variable, "{{variable}}").get mustBe Variable("variable", escape = true)
      mustache.parse(mustache.variable, "{{ variable123.name_with_underscore }}").get mustBe Variable(
        "variable123.name_with_underscore",
        escape = true
      )
    }

    "parse unescaped variable" in new Fixtures {
      // invalid
      mustache.parse(mustache.unescapedVariable, "") mustBe a[Failure]

      // valid
      mustache.parse(mustache.unescapedVariable, "{{& variable }}").get mustBe Variable("variable", escape = false)
      mustache.parse(mustache.unescapedVariable, "{{&variable}}").get mustBe Variable("variable", escape = false)
      mustache.parse(mustache.unescapedVariable, "{{& variable.name }}").get mustBe Variable(
        "variable.name",
        escape = false
      )
    }

    "parse comments" in new Fixtures {
      // invalid
      mustache.parse(mustache.comment, "") mustBe a[Failure]

      // valid
      mustache.parse(mustache.comment, "{{! comment }}").get mustBe Comment("comment")
      mustache.parse(mustache.comment, "{{!comment}}").get mustBe Comment("comment")
      mustache.parse(mustache.comment, "{{! comment\nmultiline }}").get mustBe Comment("comment\nmultiline")
    }

    "parse document with mustache" in new Fixtures {
      // invalid
      mustache.parse(mustache.template, "").get mustBe Template(Nil) // TODO ?

      // valid
      mustache.parse(mustache.template, multiline).get mustBe Template(
        List(
          Literal(
            """<!DOCTYPE html>
            |<html lang="en">
            |  <head>
            |    <meta charset="utf-8">
            |    <title>""".stripMargin
          ),
          Variable("title", escape = true),
          Literal(
            """</title>
            |    <link rel="stylesheet" href="style.css">
            |    <script src="script.js">""".stripMargin
          ),
          Comment("mustache comment"),
          Literal(
            """</script>
            |  </head>
            |  <body>
            |    <!-- page content -->
            |    """.stripMargin
          ),
          Variable("content.page", escape = false),
          Literal(
            """
            |  </body>
            |</html>""".stripMargin
          )
        )
      )
    }

    "return error for invalid template" in new Fixtures {
      mustache.parse("Content {{ variable }.").left.value mustBe a[MustacheError]
    }

    "render tempate with given context" in new Fixtures {
      val template = Template(
        List(
          Literal("Content with "),
          Variable("title", escape = true),
          Literal(" included.")
        )
      )

      case class Context(title: String)
      val context = Context("<variable>")

      mustache.render(template, (_) => Some(context.title), validate = true) mustBe Right(
        "Content with &lt;variable&gt; included."
      )
    }
  }

  "validate a template with given context" in new Fixtures {
    val template = Template(
      List(
        Variable("variable1", escape = true),
        Variable("variable2", escape = true),
        Literal(" literal")
      )
    )

    val invalidContext = (variable: String) =>
      variable match {
        case "variable1" => Some("content1")
        case _ => None
      }

    val validContext = (variable: String) =>
      variable match {
        case "variable1" => Some("content1")
        case "variable2" => Some("content2")
        case _ => None
      }

    template.isValid(invalidContext) mustBe Validated.Invalid(
      NonEmptyList.one(
        MustacheValidationError("Variable not found: variable2")
      )
    )

    template.isValid(validContext) mustBe Validated.Valid(())

    // Check with the top level render metchod
    mustache.render(template, invalidContext, validate = true) mustBe Left(
      MustacheValidationError("Variable not found: variable2")
    )
    mustache.render(template, invalidContext, validate = false) mustBe Right("content1 literal")
  }

  "escape html" in {
    Mustache.escapeHtml("") mustBe ""
    Mustache.escapeHtml("test") mustBe "test"
    Mustache.escapeHtml(
      """<test id="name" class='name' style=`/&`>"""
    ) mustBe "&lt;test id&#x3D;&quot;name&quot; class&#x3D;&#39;name&#39; style&#x3D;&#x60;&#x2F;&amp;&#x60;&gt;"
  }

  trait Fixtures {
    val mustache = new Mustache

    val multilineWithoutTags = """
                    |<!DOCTYPE html>
                    |<html lang="en">
                    |  <head>
                    |    <meta charset="utf-8">
                    |    <title>title</title>
                    |    <link rel="stylesheet" href="style.css">
                    |    <script src="script.js"></script>
                    |  </head>
                    |  <body>
                    |    <!-- page content -->
                    |  </body>
                    |</html>
                    """.stripMargin.trim
    val singlelineWithoutTags =
      """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>title</title><link rel="stylesheet" href="style.css"><script src="script.js"></script></head><body><!-- page content --></body></html>"""

    val multiline = """
                    |<!DOCTYPE html>
                    |<html lang="en">
                    |  <head>
                    |    <meta charset="utf-8">
                    |    <title>{{ title }}</title>
                    |    <link rel="stylesheet" href="style.css">
                    |    <script src="script.js">{{! mustache comment }}</script>
                    |  </head>
                    |  <body>
                    |    <!-- page content -->
                    |    {{& content.page }}
                    |  </body>
                    |</html>
                    """.stripMargin.trim
  }

}
