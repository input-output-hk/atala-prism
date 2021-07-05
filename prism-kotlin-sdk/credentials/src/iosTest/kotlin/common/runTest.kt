package io.iohk.atala.prism.kotlin.credentials.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun CoroutineScope.runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }
