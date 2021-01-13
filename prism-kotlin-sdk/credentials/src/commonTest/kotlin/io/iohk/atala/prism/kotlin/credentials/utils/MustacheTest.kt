package io.iohk.atala.prism.kotlin.credentials.utils

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlin.test.*

class MustacheTest {
    @Test
    fun testParsingEmptyString() {
        assertEquals(Template(emptyList()), Mustache.mustacheGrammar.parseToEnd(""))
    }

    @Test
    fun testParsingInvalidStrings() {
        assertFails { Mustache.mustacheGrammar.parseToEnd("{{") }
        assertFails { Mustache.mustacheGrammar.parseToEnd("}}") }
        assertFails { Mustache.mustacheGrammar.parseToEnd("Content {{ variable }.") }
    }

    @Test
    fun testParsingLiterals() {
        assertEquals(Template(listOf(Literal("."))), Mustache.mustacheGrammar.parseToEnd("."))
        assertEquals(Template(listOf(Literal("!"))), Mustache.mustacheGrammar.parseToEnd("!"))
        assertEquals(Template(listOf(Literal("&"))), Mustache.mustacheGrammar.parseToEnd("&"))
        assertEquals(Template(listOf(Literal("<div>any content</div>"))), Mustache.mustacheGrammar.parseToEnd("<div>any content</div>"))
        assertEquals(Template(listOf(Literal(singlelineWithoutTags))), Mustache.mustacheGrammar.parseToEnd(singlelineWithoutTags))
        assertEquals(Template(listOf(Literal(multilineWithoutTags))), Mustache.mustacheGrammar.parseToEnd(multilineWithoutTags))
    }

    @Test
    fun testParsingVariables() {
        assertEquals(Template(listOf(Variable("variable", escape = true))), Mustache.mustacheGrammar.parseToEnd("{{ variable }}"))
        assertEquals(Template(listOf(Variable("variable", escape = true))), Mustache.mustacheGrammar.parseToEnd("{{variable}}"))
        assertEquals(
            Template(listOf(Variable("variable123.name_with_underscore", escape = true))),
            Mustache.mustacheGrammar.parseToEnd("{{ variable123.name_with_underscore }}")
        )
    }

    @Test
    fun testParsingUnescapedVariables() {
        assertEquals(Template(listOf(Variable("variable", escape = false))), Mustache.mustacheGrammar.parseToEnd("{{& variable }}"))
        assertEquals(Template(listOf(Variable("variable", escape = false))), Mustache.mustacheGrammar.parseToEnd("{{&variable}}"))
        assertEquals(Template(listOf(Variable("variable123.name", escape = false))), Mustache.mustacheGrammar.parseToEnd("{{& variable123.name }}"))
    }

    @Test
    fun testParsingComments() {
        assertEquals(Template(listOf(Comment("comment"))), Mustache.mustacheGrammar.parseToEnd("{{! comment }}"))
        assertEquals(Template(listOf(Comment("comment"))), Mustache.mustacheGrammar.parseToEnd("{{!comment}}"))
        assertEquals(Template(listOf(Comment("comment\nmultiline"))), Mustache.mustacheGrammar.parseToEnd("{{! comment\nmultiline }}"))
    }

    @Test
    fun testParsing() {
        assertEquals(
            Template(
                listOf(
                    Literal(
                        """<!DOCTYPE html>
                        |<html lang="en">
                        |  <head>
                        |    <meta charset="utf-8">
                        |    <title>""".trimMargin()
                    ),
                    Variable("title", escape = true),
                    Literal(
                        """</title>
                        |    <link rel="stylesheet" href="style.css">
                        |    <script src="script.js">""".trimMargin()
                    ),
                    Comment("mustache comment"),
                    Literal(
                        """</script>
                        |  </head>
                        |  <body>
                        |    <!-- page content -->
                        |    """.trimMargin()
                    ),
                    Variable("content.page", escape = false),
                    Literal(
                        """
                        |
                        |  </body>
                        |</html>""".trimMargin()
                    )
                )
            ),
            Mustache.mustacheGrammar.parseToEnd(multiline)
        )
    }

    @Test
    fun testRenderTemplateWithGivenContext() {
        val template = Template(
            listOf(
                Literal("Content with "),
                Variable("title", escape = true),
                Literal(" included.")
            )
        )
        val stringTemplate = "Content with {{ title }} included."
        val context = { _: String -> "variable" }

        assertEquals("Content with variable included.", Mustache.render(template, context))
        assertEquals("Content with variable included.", Mustache.render(stringTemplate, context))
    }

    @Test
    fun testRenderWithEscaping() {
        val template = "{{ variable1 }} {{& variable2 }}"
        val context = { _: String -> "<br />" }

        assertEquals("&lt;br &#x2F;&gt; <br />", Mustache.render(template, context))
    }

    @Test
    fun testEscapeHtml() {
        assertEquals("", Mustache.escapeHtml(""))
        assertEquals("test", Mustache.escapeHtml("test"))
        assertEquals("&lt;test id&#x3D;&quot;name&quot; class&#x3D;&#39;name&#39; style&#x3D;&#x60;&#x2F;&amp;&#x60;&gt;", Mustache.escapeHtml("""<test id="name" class='name' style=`/&`>"""))
    }

    val multilineWithoutTags =
        """<!DOCTYPE html>
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
        """.trimMargin().trim()

    val singlelineWithoutTags =
        """<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>title</title><link rel="stylesheet" href="style.css"><script src="script.js"></script></head><body><!-- page content --></body></html>"""

    val multiline =
        """<!DOCTYPE html>
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
        """.trimMargin().trim()
}
