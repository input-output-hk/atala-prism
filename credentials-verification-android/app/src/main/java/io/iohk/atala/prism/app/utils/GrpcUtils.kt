package io.iohk.atala.prism.app.utils

import com.google.protobuf.ByteString
import io.grpc.Metadata
import io.iohk.atala.prism.crypto.japi.ECKeyPair
import io.iohk.atala.prism.protos.EncodedPublicKey

class GrpcUtils {
    companion object {
        val REQUEST_NONCE_KEY: Metadata.Key<String> = Metadata.Key.of("requestnonce", Metadata.ASCII_STRING_MARSHALLER)
        val SIGNATURE_KEY: Metadata.Key<String> = Metadata.Key.of("signature", Metadata.ASCII_STRING_MARSHALLER)
        val PUBLIC_KEY: Metadata.Key<String> = Metadata.Key.of("publickey", Metadata.ASCII_STRING_MARSHALLER)

        fun getPublicKeyEncoded(ecKeyPair: ECKeyPair): EncodedPublicKey {
            return EncodedPublicKey.newBuilder().setPublicKey(ByteString.copyFrom(ecKeyPair.public.encoded)).build()
        }
    }
}