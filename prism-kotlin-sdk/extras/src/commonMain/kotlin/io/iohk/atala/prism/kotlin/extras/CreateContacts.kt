package io.iohk.atala.prism.kotlin.extras

import com.benasher44.uuid.bytes
import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.protos.*
import io.iohk.atala.prism.kotlin.protos.util.Base64Utils
import kotlinx.serialization.json.*
import pbandk.encodeToByteArray

sealed class InvalidDataException(message: String) : Exception(message)

object EmptyDataException : InvalidDataException("Data is empty. Header should be presented in the first row.")

data class MandatoryFieldNotFound(val field: String) : InvalidDataException("Mandatory field [$field] was not found")

data class InvalidRow(val index: Int) : InvalidDataException("Row $index is invalid")

data class DuplicateExternalId(val id: String, val row1: Int, val row2: Int) : InvalidDataException("Duplicate externalId $id found on rows $row1 and $row2")

suspend fun createContacts(
    csvData: List<List<String>>,
    groups: List<String>,
    getMetadata: suspend (ByteArray, ByteArray) -> ConnectorRequestMetadata,
    contactsService: ContactsServiceCoroutine,
): Int {
    val reqUnsigned = createContactsUnsigned(csvData, groups)
    val nonce = uuid4().bytes

    val generateConnectionTokenRequest = GenerateConnectionTokenRequest(reqUnsigned.contacts.size)
    val connectorRequestMetadata = getMetadata(generateConnectionTokenRequest.encodeToByteArray(), nonce)

    val req = CreateContactsRequest(reqUnsigned.groups, reqUnsigned.contacts, connectorRequestMetadata)
    val metadata = getMetadata(req.encodeToByteArray(), nonce)
    val prismMetadata = PrismMetadata(metadata.did, metadata.didKeyId, Base64Utils.decode(metadata.didSignature), nonce)

    val response = contactsService.CreateContactsAuth(req, prismMetadata)
    return response.contactsCreated
}

internal fun createContactsUnsigned(dataLines: List<List<String>>, groups: List<String>): CreateContactsRequest {
    if (dataLines.isEmpty()) {
        throw EmptyDataException
    }

    val header = dataLines.first().map { it.trim() }

    val indexExternalId = header.indexOf("externalId")
    if (indexExternalId < 0) {
        throw MandatoryFieldNotFound("externalId")
    }

    val indexName = header.indexOf("contactName")
    if (indexName < 0) {
        throw MandatoryFieldNotFound("contactName")
    }

    val headerOptionalFields = header.filterIndexed { index, _ ->
        index != indexName && index != indexExternalId
    }

    val contacts = dataLines
        .drop(1)
        .withIndex()
        .filterNot { it.value.isEmpty() }
        .map { (rowIndex, rowStr) ->
            val row = rowStr.map { it.trim() }
            if (row.size != header.size) {
                throw InvalidRow(rowIndex + 1)
            }
            val name = row[indexName]
            val externalId = row[indexExternalId]
            val optionalFields = row.filterIndexed { colIndex, _ ->
                colIndex != indexName && colIndex != indexExternalId
            }
            val jsonData = JsonObject(
                headerOptionalFields.zip(optionalFields.map { JsonPrimitive(it) }).toMap()
            ).toString()
            CreateContactsRequest.Contact(externalId, name, jsonData)
        }

    validateExternalIdUnique(contacts)
    return CreateContactsRequest(groups, contacts, null)
}

internal fun validateExternalIdUnique(contacts: List<CreateContactsRequest.Contact>) {
    contacts
        .map { it.externalId }
        .withIndex()
        .sortedBy { it.value }
        .zipWithNext()
        .forEach { (cur, nxt) ->
            if (cur.value == nxt.value) {
                throw DuplicateExternalId(cur.value, cur.index + 1, nxt.index + 1)
            }
        }
}
