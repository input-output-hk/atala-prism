package io.iohk.atala.prism.app.neo.common

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.Continuation

suspend inline fun <T> suspendCancellableCoroutineWithTimeoutOrNull(
    timeoutMillis: Long,
    crossinline block: (Continuation<T>) -> Unit
): T? {
    var result: T? = null
    withTimeoutOrNull(timeMillis = timeoutMillis) {
        result = suspendCancellableCoroutine(block = block)
    }
    return result
}

suspend inline fun <T> suspendCancellableCoroutineWithTimeout(
    timeoutMillis: Long,
    crossinline block: (Continuation<T>) -> Unit
): T {
    var result: T
    withTimeout(timeMillis = timeoutMillis) {
        result = suspendCancellableCoroutine(block = block)
    }
    return result
}
