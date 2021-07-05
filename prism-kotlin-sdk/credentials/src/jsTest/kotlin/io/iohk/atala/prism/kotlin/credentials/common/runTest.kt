package io.iohk.atala.prism.kotlin.credentials.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise

actual fun CoroutineScope.runTest(block: suspend CoroutineScope.() -> Unit): dynamic = promise { block() }
