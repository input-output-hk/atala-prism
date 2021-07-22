package io.iohk.atala

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import io.iohk.atala.prism.app.neo.common.extensions.getOrAwaitValueOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class LiveDataGetOrAwaitValueOrThrowExtensionTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test(expected = TimeoutCancellationException::class)
    fun outOfTimeTest() = runBlocking {
        val liveData = MutableLiveData<String>()
        postValueDelayed(liveData, "Hello world", 3000)

        val result: String = liveData.getOrAwaitValueOrThrow(2000)
    }

    @Test
    fun inTimeTest() = runBlocking {
        val liveData = MutableLiveData<String>()
        postValueDelayed(liveData, "Hello world", 3000)

        val result: String = liveData.getOrAwaitValueOrThrow(3500)

        Assert.assertEquals(result, "Hello world")
    }

    private fun <T> postValueDelayed(mutableLiveData: MutableLiveData<T>, value: T, timeMillis: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(timeMillis)
            mutableLiveData.value = value
        }
    }
}
