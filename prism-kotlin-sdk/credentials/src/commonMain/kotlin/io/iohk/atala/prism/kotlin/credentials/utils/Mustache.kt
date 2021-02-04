package io.iohk.atala.prism.kotlin.credentials.utils

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.*
import com.github.h0tk3y.betterParse.lexer.*
import com.github.h0tk3y.betterParse.parser.*

sealed class MustacheNode
data class Literal(val content: String) : MustacheNode()
data class Comment(val content: String) : MustacheNode()
data class Variable(val name: String, val escape: Boolean) : MustacheNode()

data class Template(val tags: List<MustacheNode>) {
    /**
     * Validate template with the given context.
     */
    fun validate(context: TemplateContext): List<MustacheValidationError> {
        return tags.mapNotNull { tag ->
            when (tag) {
                is Variable ->
                    if (context(tag.name) == null) MustacheValidationError("Variable not found: ${tag.name}") else null

                else -> null
            }
        }
    }
}

sealed class MustacheError(message: String) : Exception(message)
class MustacheParsingError(message: String) : MustacheError(message)
data class MustacheValidationError(
    override val message: String,
    val errors: List<MustacheValidationError> = emptyList()
) : MustacheError(message)

typealias TemplateContext = (String) -> String?

/**
 * Subset of Mustache templates (https://mustache.github.io/) for internal usage in Prism SDK.
 *
 * Implemented specification follows https://mustache.github.io/mustache.5.html
 *
 * Currently supported tags:
 *   - variables: {{ variable }} or with path {{ variable.name }}
 *   - unescaped variables: {{& variable }}
 *   - comments with multiline support: {{! variable }}
 *
 * Usage:
 * {{{
 *   val context = { variable: String -> "$variable value" }
 *   Mustache.render("Template with {{ variable }}.", context) == "Template with variable value."
 * }}}
 */
object Mustache {
    val mustacheGrammar = object : Grammar<Template>() {
        val open by literalToken("{{")
        val close by literalToken("}}")
        val dot by literalToken(".")
        val andSign by literalToken("&")
        val bangSign by literalToken("!")
        val ws by regexToken("\\s+")
        val identifierToken by regexToken("([a-zA-Z\\d_]*)")
        val anyCharacter by regexToken(".|\r|\n")

        val literalString by oneOrMore(
            ws or dot or andSign or bangSign or identifierToken or anyCharacter
        ) use { joinToString("") { it.text } }
        val literal by literalString map { Literal(it) }

        val identifier by separatedTerms(
            identifierToken,
            dot,
            acceptZero = true
        ) use { joinToString(".") { it.text } }

        val variable by mustache(pad(identifier) map { Variable(it, escape = true) })

        val unescapedVariable by mustache(-andSign and pad(identifier) map { Variable(it, escape = false) })

        val comment by mustache(-bangSign and pad(literalString) map { Comment(it.trim()) })

        val statements by variable or unescapedVariable or comment

        override val rootParser: Parser<Template>
            get() = zeroOrMore(literal or statements) map { Template(it) }

        private fun pad(parser: Parser<String>): Parser<String> =
            -optional(ws) and parser and -optional(ws)

        private fun mustache(parser: Parser<MustacheNode>): Parser<MustacheNode> =
            -open and parser and -close
    }

    fun render(template: Template, context: TemplateContext, validate: Boolean): String {
        if (validate) {
            val errors = template.validate(context)
            if (errors.isNotEmpty()) {
                throw MustacheValidationError(
                    "Given context doesn't contain all variables from the template.",
                    errors
                )
            }
        }

        return template.tags.joinToString("") {
            when (it) {
                is Literal -> it.content
                is Comment -> ""
                is Variable ->
                    when (it.escape) {
                        true -> escapeHtml(context(it.name) ?: "")
                        false -> context(it.name) ?: ""
                    }
            }
        }
    }

    fun render(template: String, context: TemplateContext, validate: Boolean = true): String =
        try {
            render(mustacheGrammar.parseToEnd(template), context, validate)
        } catch (e: ParseException) {
            throw MustacheParsingError(e.message!!)
        }

    /**
     * Escape html entities.
     * Solution from: https://github.com/janl/mustache.js/blob/master/mustache.js#L67-L76
     */
    fun escapeHtml(html: String): String =
        buildString {
            html.forEach {
                when (it) {
                    '\'' -> append("&#39;")
                    '\"' -> append("&quot;")
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '/' -> append("&#x2F;")
                    '`' -> append("&#x60;")
                    '=' -> append("&#x3D;")
                    else -> append(it)
                }
            }
        }
}
