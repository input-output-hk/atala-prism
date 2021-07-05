package io.iohk.atala.prism.kotlin.credentials.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

expect fun CoroutineScope.runTest(block: suspend CoroutineScope.() -> Unit)

@OptIn(DelicateCoroutinesApi::class)
fun <R> runThenAssert(testee: suspend () -> R, assertion: (R) -> Unit) =
    GlobalScope.runTest {
        assertion(testee())
    }
