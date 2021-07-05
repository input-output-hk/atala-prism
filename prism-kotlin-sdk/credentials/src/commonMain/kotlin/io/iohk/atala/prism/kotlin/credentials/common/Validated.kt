package io.iohk.atala.prism.kotlin.credentials.common

sealed class Validated<out R, E> {
    data class Valid<out R, E>(override val result: R) : Validated<R, E>()
    data class Invalid<out R, E>(override val error: E) : Validated<R, E>()

    open val result: R? = null
    open val error: E? = null

    fun toTuple(): Pair<R?, E?> = Pair(result, error)

    fun <R1> flatMap(map: (r: R) -> Validated<R1, E>): Validated<R1, E> =
        when (this) {
            is Valid -> map(this.result)
            is Invalid -> Invalid(this.error)
        }

    suspend fun <R1> suspendableFlatMap(map: suspend (r: R) -> Validated<R1, E>): Validated<R1, E> =
        when (this) {
            is Valid -> map(this.result)
            is Invalid -> Invalid(this.error)
        }

    fun <R1> map(map: (r: R) -> R1): Validated<R1, E> =
        when (this) {
            is Valid -> Validated.Valid(map(this.result))
            is Invalid -> Invalid(this.error)
        }

    suspend fun <R1> suspendableMap(map: suspend (r: R) -> R1): Validated<R1, E> =
        when (this) {
            is Valid -> Validated.Valid(map(this.result))
            is Invalid -> Invalid(this.error)
        }

    object Applicative {
        fun <R, R1, R2, E> apply(
            validated1: Validated<R1, E>,
            validated2: Validated<R2, E>,
            apply: (r1: R1, r2: R2) -> Validated<R, E>
        ): Validated<R, E> = validated1.flatMap {
            val r1 = it
            validated2.flatMap {
                val r2 = it
                apply(r1, r2)
            }
        }
    }
}
