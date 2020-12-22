package io.iohk.atala.prism.kotlin.protos

import io.iohk.atala.prism.protos.GetConnectionByTokenRequest
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtoTest {
    @Test
    fun testProtobufModelEncoding() {
        val request = GetConnectionByTokenRequest(token = "123")
        val decodedRequest = GetConnectionByTokenRequest.decodeFromByteArray(request.encodeToByteArray())
        assertEquals(request, decodedRequest)
    }
}
