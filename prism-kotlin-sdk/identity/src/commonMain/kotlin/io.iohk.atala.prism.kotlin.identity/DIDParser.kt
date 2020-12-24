package io.iohk.atala.prism.kotlin.identity

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.*
import com.github.h0tk3y.betterParse.parser.Parser

object DIDParser {
    private fun Parser<List<String>>.joinToString(): Parser<String> =
        this.use { joinToString(separator = "") }

    private infix operator fun Parser<String>.plus(other: Parser<String>) =
        (this * other).use { t1 + t2 }

    val DID_SCHEMA = literalToken("did")
    val ALPHA_LOWER = regexToken("[a-z]")
    val ALPHA_HEX = regexToken("[A-F]")
    val ALPHA_NON_HEX_UPPER = regexToken("[G-Z]")
    val ALPHA_UPPER = ALPHA_HEX or ALPHA_NON_HEX_UPPER
    val ALPHA = ALPHA_LOWER or ALPHA_UPPER
    val DIGIT = regexToken("[0-9]")
    val DOT = literalToken(".")
    val HYPHEN = literalToken("-")
    val UNDERSCORE = literalToken("_")
    val COLON = literalToken(":")
    val TILDE = literalToken("~")
    val PERCENT = literalToken("%")
    val AT = literalToken("@")
    val FORWARD_SLASH = literalToken("/")
    val QUESTION_MARK = literalToken("?")
    val HASH = literalToken("#")
    val SUBDELIMITER = regexToken("""[!\\$&'()*+,;=]""")
    val sharedTokens = listOf(
        DID_SCHEMA, ALPHA_LOWER, ALPHA_HEX, ALPHA_NON_HEX_UPPER, DIGIT, DOT, HYPHEN, UNDERSCORE,
        COLON, TILDE, PERCENT, AT, FORWARD_SLASH, QUESTION_MARK, HASH, SUBDELIMITER
    )

    // As defined in https://www.w3.org/TR/did-core/#did-syntax
    val didGrammar = object : Grammar<DID>() {
        override val tokens: List<Token>
            get() = super.tokens + sharedTokens

        val didSchema by DID_SCHEMA.use { text }
        val colon by COLON.use { text }
        val methodChar by (ALPHA_LOWER or DIGIT).use { text }
        val methodName by oneOrMore(methodChar).joinToString()

        val idChar by (ALPHA or DIGIT or DOT or HYPHEN or UNDERSCORE).use { text }
        val idChars0 by oneOrMore(idChar).joinToString()
        val idChars1 by oneOrMore(idChar).joinToString()
        val optionalSegments by zeroOrMore(idChars0 * COLON).use { joinToString { it.t1 + it.t2.text } }
        val methodSpecificId by (optionalSegments * idChars1).use { t1 + t2 }

        override val rootParser by
        (didSchema + colon + methodName + colon + methodSpecificId).map { DID.fromString(it) }
    }

    // As defined in https://www.w3.org/TR/did-core/#did-url-syntax
    val didUrlGrammar = object : Grammar<DIDUrl>() {
        override val tokens: List<Token>
            get() = super.tokens + sharedTokens

        val colon by COLON.use { text }
        val at by AT.use { text }
        val forwardSlash by FORWARD_SLASH.use { text }
        val questionMark by QUESTION_MARK.use { text }
        val hash by HASH.use { text }

        val did = didGrammar.rootParser

        val hexDig = (DIGIT or ALPHA_HEX).use { text }

        // As defined in https://tools.ietf.org/html/rfc3986#section-2.3
        val unreservedChar = (ALPHA or DIGIT or HYPHEN or DOT or UNDERSCORE or TILDE).use { text }

        // As defined in https://tools.ietf.org/html/rfc3986#section-2.1
        val pctEncoded = PERCENT.use { text } + hexDig + hexDig

        // As defined in https://tools.ietf.org/html/rfc3986#section-2.2
        val subDelims = SUBDELIMITER.use { text }

        // As defined in https://tools.ietf.org/html/rfc3986#section-3.3
        val pchar = unreservedChar or pctEncoded or subDelims or colon or at
        val segment = zeroOrMore(pchar).joinToString()

        val pathAbempty = zeroOrMore(-forwardSlash * segment)

        // As defined in https://tools.ietf.org/html/rfc3986#section-3.4
        val query = zeroOrMore(pchar or forwardSlash or questionMark).joinToString()
        val queryMap = query.map { queryString ->
            queryString.split('&').map {
                val index = it.indexOf('=')
                val key = if (index > 0) it.substring(0 until index) else it
                val value =
                    if (index > 0 && it.length > index + 1)
                        it.substring(index + 1 until it.length)
                    else
                        null
                Pair(key, value)
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.filterNotNull() }
        }

        // As defined in https://tools.ietf.org/html/rfc3986#section-3.5
        val fragment = zeroOrMore(pchar or forwardSlash or questionMark).joinToString()

        val didUrl =
            did * pathAbempty * optional(-questionMark * queryMap) * optional(-hash * fragment)

        override val rootParser by didUrl.map { DIDUrl(it.t1, it.t2, it.t3 ?: emptyMap(), it.t4) }
    }
}
