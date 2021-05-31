package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.protos.ConnectorRequestMetadata
import io.iohk.atala.prism.kotlin.protos.ContactsServiceJS
import kotlinx.coroutines.*
import kotlin.js.Promise

@JsExport
fun createContactsRequestJS(
    csvData: Array<Array<String>>,
    groups: Array<String>,
    getMetadataJS: (ByteArray, ByteArray) -> Promise<ConnectorRequestMetadata>,
    contactsService: ContactsServiceJS
): Promise<Int> {
    return GlobalScope.promise {
        createContacts(
            csvData.toList().map { it.toList() },
            groups.toList(),
            getMetadata = { request, nonce -> getMetadataJS(request, nonce).await() },
            contactsService = contactsService.internalService
        )
    }
}

@JsExport
fun createConnectorRequestMetadataJS(
    did: String,
    didKeyId: String,
    didSignature: String,
    requestNonce: String
): ConnectorRequestMetadata {
    return ConnectorRequestMetadata(did, didKeyId, didSignature, requestNonce)
}
