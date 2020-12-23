package io.iohk.atala.prism.kotlin.identity

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed

data class DIDUrl(
    val did: DID,
    val path: List<String>,
    val parameters: Map<String, List<String>>,
    val fragment: String?
) {
    companion object {
        fun fromString(rawDidUrl: String): DIDUrl =
            when (val result = DIDParser.didUrlGrammar.tryParseToEnd(rawDidUrl)) {
                is Parsed -> result.value
                is ErrorResult -> {
                    println(result)
                    throw IllegalArgumentException("Invalid DID URL: $rawDidUrl")
                }
            }
    }

    val keyId: String?
        get() =
            if (path.size == 2 && path[0] == "keyId") {
                path[1]
            } else {
                null
            }
}