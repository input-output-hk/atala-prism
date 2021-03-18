package io.iohk.atala.prism.kotlin.protos

import kotlinx.coroutines.runBlocking

actual fun runTest(block: suspend () -> Unit): Unit =
    runBlocking { block() }
