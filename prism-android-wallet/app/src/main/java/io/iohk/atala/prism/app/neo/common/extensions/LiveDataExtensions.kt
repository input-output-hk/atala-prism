package io.iohk.atala.prism.app.neo.common.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.iohk.atala.prism.app.neo.common.suspendCancellableCoroutineWithTimeout
import io.iohk.atala.prism.app.neo.common.suspendCancellableCoroutineWithTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.resume
import kotlin.jvm.Throws

/**
 * If the current [LiveData.getValue] is null waits [timeMillis] milliseconds for a value unequal to null
 * if the timeout is over returns null
 *
 * @param timeMillis [Long]
 * @return returns the value of the [LiveData]
 */
suspend fun <T> LiveData<T>.getOrAwaitValue(timeMillis: Long): T? = suspendCancellableCoroutineWithTimeoutOrNull(timeMillis) { continuation ->
    val observer = object : Observer<T> {
        override fun onChanged(v: T?) {
            v?.let {
                this@getOrAwaitValue.removeObserver(this)
                continuation.resume(v)
            }
        }
    }
    this@getOrAwaitValue.observeForever(observer)
}

/**
 * If the current [LiveData.getValue] is null waits [timeMillis] milliseconds for a value unequal to null
 * if the timeout is over throw an [TimeoutCancellationException]
 *
 * @param timeMillis [Long]
 * @return returns the value of the [LiveData]
 */
@Throws(TimeoutCancellationException::class)
suspend fun <T> LiveData<T>.getOrAwaitValueOrThrow(timeMillis: Long): T? = suspendCancellableCoroutineWithTimeout(timeMillis) { continuation ->
    val observer = object : Observer<T> {
        override fun onChanged(v: T?) {
            v?.let {
                this@getOrAwaitValueOrThrow.removeObserver(this)
                continuation.resume(v)
            }
        }
    }
    this@getOrAwaitValueOrThrow.observeForever(observer)
}
