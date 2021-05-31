package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.protos.CreateContactsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateContactsTest {
    @Test
    fun externalIdUniqueTest() {
        val contacts = mutableListOf(
            CreateContactsRequest.Contact("12345", "Student1", "{}"),
            CreateContactsRequest.Contact("12346", "Student2", "{}"),
            CreateContactsRequest.Contact("12347", "Student3", "{}")
        )
        validateExternalIdUnique(contacts)

        contacts.add(CreateContactsRequest.Contact("12347", "Student3", "{}"))
        assertFailsWith<DuplicateExternalId> {
            validateExternalIdUnique(contacts)
        }
    }

    @Test
    fun createContactsValidationTest() {
        assertFailsWith<EmptyDataException> {
            createContactsUnsigned(emptyList(), emptyList())
        }

        assertFailsWith<MandatoryFieldNotFound> {
            createContactsUnsigned(listOf(emptyList()), listOf("group1"))
        }

        assertFailsWith<MandatoryFieldNotFound> {
            createContactsUnsigned(listOf(listOf("externalId")), listOf("group1"))
        }

        val header = listOf("externalId", "contactName", "field1", "field2")
        val row1 = listOf("123456", "Student", "Bad", "Quite")
        val row2 = emptyList<String>() // empty rows will be ignored by createContactsUnsigned
        val row3 = listOf("123457", "Student2", "Good", "Quite")
        val groups = listOf("group1", "group2")
        assertFailsWith<InvalidRow> {
            createContactsUnsigned(listOf(header, row1, listOf("incorrectRow:)")), listOf("group"))
        }
        val contactsRequest = createContactsUnsigned(listOf(header, row1, row2, row3), groups)
        assertEquals(groups.toList(), contactsRequest.groups)
        assertEquals(2, contactsRequest.contacts.size)

        val jsonData =
            """{"field1":"${row1[2]}","field2":"${row1[3]}"}""".trimIndent()
        val firstContact = CreateContactsRequest.Contact(row1[0], row1[1], jsonData)
        assertEquals(firstContact, contactsRequest.contacts[0])
    }
}
