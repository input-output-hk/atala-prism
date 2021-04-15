package io.iohk.atala.prism.app.neo.common.extensions

import com.google.protobuf.Timestamp
import java.time.Instant

fun Timestamp.toMilliseconds(): Long = Instant.ofEpochSecond(seconds, nanos.toLong()).toEpochMilli()
