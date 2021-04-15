package io.iohk.atala.prism.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.base.Charsets
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.local.db.AppDatabase
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.IOException
import java.io.InvalidObjectException
import java.util.Date
import java.util.concurrent.Executors

@RunWith(BlockJUnit4ClassRunner::class)
class ContactDaoTest {
    private lateinit var contactDao: ContactDao

    private lateinit var credentialDao: CredentialDao

    private lateinit var db: AppDatabase

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .setQueryExecutor(testDispatcher.asExecutor()).build()
        contactDao = db.contactDao()
        credentialDao = db.credentialDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Test for [ContactDao.insert] method, this test tries to verify that the [Contact] record is created correctly as well
     * as a record of an [ActivityHistory] of type [ActivityHistory.Type.ContactAdded] related to the created [Contact]
     * */
    @Test
    @Throws(Exception::class)
    fun insertAndSelect() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")

        val contactID = contactDao.insert(contact)

        val contactByConnectionId = contactDao.getContactByConnectionId("connection1")
        MatcherAssert.assertThat(contactByConnectionId!!.connectionId, Matchers.equalTo("connection1"))
        val contactById = contactDao.contactById(contactID.toInt())!!
        MatcherAssert.assertThat(contactById!!.connectionId, Matchers.equalTo("connection1"))
        val activities = contactDao.getActivityHistoriesByConnection("connection1")
        MatcherAssert.assertThat("Total of activities is wrong", activities.size == 1)
        val createdContactActivityHistory = activities[0].activityHistory
        MatcherAssert.assertThat("The type of activity is wrong", createdContactActivityHistory.type == ActivityHistory.Type.ContactAdded)
    }

    /**
     * Test for [ContactDao.insertAll] this test tries to verify that the records of all [Contact]'s have been created correctly as well
     * as the records of all their [ActivityHistory]'s of type [ActivityHistory.Type.ContactAdded]
     * */
    @Test
    @Throws(Exception::class)
    fun insertAllAndGetAll() = runBlocking {
        contactDao.removeAllData()
        contactDao.removeAllData()

        contactDao.insertAll(
            listOf(
                buildSimpleContact("connection1", "contact 1"),
                buildSimpleContact("connection2", "contact 2"),
                buildSimpleContact("connection3", "contact 3")
            )
        )

        val contacts = contactDao.getAll()
        MatcherAssert.assertThat("Total of contacts is wrong", contacts.size == 3)
        val foundContact1 = contacts.find { it.connectionId == "connection1" && it.name == "contact 1" } != null
        MatcherAssert.assertThat("Contact 1 does not found", foundContact1)
        val foundContact2 = contacts.find { it.connectionId == "connection2" && it.name == "contact 2" } != null
        MatcherAssert.assertThat("Contact 2 does not found", foundContact2)
        val foundContact3 = contacts.find { it.connectionId == "connection3" && it.name == "contact 3" } != null
        MatcherAssert.assertThat("Contact 3 does not found", foundContact3)
        // Check Activities Histories from contact 1
        val activitiesContact1 = contactDao.getActivityHistoriesByConnection("connection1")
        val createdContact1ActivityHistory = activitiesContact1[0].activityHistory
        MatcherAssert.assertThat("The type of activity of contact 1 is wrong", createdContact1ActivityHistory.type == ActivityHistory.Type.ContactAdded)
        // Check Activities Histories from contact 2
        val activitiesContact2 = contactDao.getActivityHistoriesByConnection("connection2")
        val createdContact2ActivityHistory = activitiesContact2[0].activityHistory
        MatcherAssert.assertThat("The type of activity of contact 2 is wrong", createdContact2ActivityHistory.type == ActivityHistory.Type.ContactAdded)
        // Check Activities Histories from contact 3
        val activitiesContact3 = contactDao.getActivityHistoriesByConnection("connection3")
        val createdContact3ActivityHistory = activitiesContact3[0].activityHistory
        MatcherAssert.assertThat("The type of activity of contact 3 is wrong", createdContact3ActivityHistory.type == ActivityHistory.Type.ContactAdded)
    }

    /**
     * Test for [ContactDao.insert] method, this test tries to verify that the [Contact] record is created correctly with all they
     * issued [Credential]'s as well as all the required [ActivityHistory]'s of type [ActivityHistory.Type.ContactAdded] and
     * [ActivityHistory.Type.CredentialIssued] related to the created [Contact]
     * */
    @Test
    @Throws(Exception::class)
    fun insertContactWithIssuedCredentials() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        val credential2 = buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        val credential3 = buildSimpleCredential("connection1", "Contact 1", "credential3", "encodedData3".toByteStringUTF8())

        val contactID = contactDao.insert(contact, listOf(credential1, credential2, credential3))

        val contactById = contactDao.contactById(contactID.toInt())!!
        MatcherAssert.assertThat("The contact was not saved correctly", contactById.connectionId == "connection1" && contactById.name == "Contact 1")
        val credentials = contactDao.getIssuedCredentials("connection1")
        MatcherAssert.assertThat("Total of credentials is wrong", credentials.size == 3)
        val foundCredential1 = credentials.find {
            it.connectionId == "connection1" && it.credentialId == "credential1"
        } != null
        MatcherAssert.assertThat("Credential 1 does not found", foundCredential1)
        val foundCredential2 = credentials.find {
            it.connectionId == "connection1" && it.credentialId == "credential2"
        } != null
        MatcherAssert.assertThat("Credential 2 does not found", foundCredential2)
        val foundCredential3 = credentials.find {
            it.connectionId == "connection1" && it.credentialId == "credential3"
        } != null
        MatcherAssert.assertThat("Credential 3 does not found", foundCredential3)
        // Check if ContactAdded activity History was created
        val allConnectionActivities = contactDao.getActivityHistoriesByConnection("connection1")
        val foundCreatedContactActivity = allConnectionActivities.find {
            it.activityHistory.type == ActivityHistory.Type.ContactAdded && it.activityHistory.connectionId == "connection1"
        }
        MatcherAssert.assertThat("ContactAdded activity history does not found", foundCreatedContactActivity != null)
        // Check if CredentialIssued activity histories was created
        val credentialsActivitiesHistories = contactDao.getCredentialsActivityHistoriesByConnection("connection1")
        MatcherAssert.assertThat("Total of credentials activities is wrong", credentialsActivitiesHistories.size == 3)

        // Check Encoded credentials data
        val credentialWithEncodedCredential1 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential1")
        MatcherAssert.assertThat(
            "Encoded credential 1 is not saved correctly",
            credentialWithEncodedCredential1?.encodedCredential?.credentialEncoded == "encodedData1".toByteStringUTF8()
        )
        val credentialWithEncodedCredential2 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential2")
        MatcherAssert.assertThat(
            "Encoded credential 2 is not saved correctly",
            credentialWithEncodedCredential2?.encodedCredential?.credentialEncoded == "encodedData2".toByteStringUTF8()
        )
        val credentialWithEncodedCredential3 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential3")
        MatcherAssert.assertThat(
            "Encoded credential 3 is not saved correctly",
            credentialWithEncodedCredential3?.encodedCredential?.credentialEncoded == "encodedData3".toByteStringUTF8()
        )
    }

    @Test(expected = InvalidObjectException::class)
    fun invalidInsertWithIssuedCredentials() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        // credential2 is an invalid credential because connectionId is different
        val credential2 = buildSimpleCredential("connection2", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        val credential3 = buildSimpleCredential("connection1", "Contact 1", "credential3", "encodedData3".toByteStringUTF8())
        val contactId = contactDao.insert(contact, listOf(credential1, credential2, credential3))
    }

    @Test
    fun insertIssuedCredentialsToAContact() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")
        val contactId = contactDao.insert(contact)
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())

        contactDao.insertIssuedCredentialsToAContact(contactId, listOf(credential1))

        // Check if credentials was created
        val credentials = contactDao.getIssuedCredentials("connection1")
        MatcherAssert.assertThat("Total of credentials is wrong", credentials.size == 1)
        val foundCredential1 = credentials.find {
            it.connectionId == "connection1" && it.credentialId == "credential1"
        } != null
        MatcherAssert.assertThat("Credential 1 does not found", foundCredential1)
        // Check if CredentialIssued activity history was created
        val credentialsActivitiesHistories = contactDao.getCredentialsActivityHistoriesByConnection("connection1")
        MatcherAssert.assertThat("Total of credentials activities is wrong", credentialsActivitiesHistories.size == 1)
        val activityHistory = credentialsActivitiesHistories[0]
        MatcherAssert.assertThat(
            "The activity was not created correctly",
            activityHistory.activityHistory.type == ActivityHistory.Type.CredentialIssued &&
                activityHistory.credential?.credentialId == "credential1"
        )
        // Check Encoded credentials data
        val encodedCredential1 = credentialDao.getEncodedCredentialByCredentialId("credential1")
        MatcherAssert.assertThat(
            "Encoded credential 1 is not saved correctly",
            encodedCredential1?.credentialEncoded == "encodedData1".toByteStringUTF8()
        )
    }

    @Test(expected = InvalidObjectException::class)
    fun insertInvalidIssuedCredentialsToAContact() = runBlocking {
        val contact = buildSimpleContact("connection1", "Contact 1")
        val contactId = contactDao.insert(contact)
        // credential1 is an invalid credential because connectionId is different
        val credential1 = buildSimpleCredential("connection2", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        contactDao.insertIssuedCredentialsToAContact(contactId, listOf(credential1))
    }

    @Test
    fun insertAllWithIssuedCredentials() = runBlocking {
        contactDao.removeAllData()
        val map: MutableMap<Contact, List<CredentialWithEncodedCredential>> = mutableMapOf()
        val contact1 = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        val credential2 = buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        map[contact1] = listOf(credential1, credential2)
        val contact2 = buildSimpleContact("connection2", "Contact 2")
        val credential3 = buildSimpleCredential("connection2", "Contact 2", "credential3", "encodedData3".toByteStringUTF8())
        map[contact2] = listOf(credential3)

        contactDao.insertAll(map)

        val contacts = contactDao.getAll()
        MatcherAssert.assertThat("Total of contacts is wrong", contacts.size == 2)
        // Check data for Contact 1
        val contact1ByConnectionId = contactDao.getContactByConnectionId("connection1")!!
        MatcherAssert.assertThat("contact 1 was not saved correctly", contact1ByConnectionId.connectionId == "connection1" && contact1ByConnectionId.name == "Contact 1")
        val contact1Credentials = contactDao.getIssuedCredentials("connection1")
        MatcherAssert.assertThat("Total of credentials in contact 1 is wrong", contact1Credentials.size == 2)
        val contactAddedActivitiesForContact1 = contactDao.getActivityHistoriesByConnection("connection1").filter {
            it.activityHistory.type == ActivityHistory.Type.ContactAdded
        }
        MatcherAssert.assertThat("Total of contact added activities in contact 1 is wrong", contactAddedActivitiesForContact1.size == 1)
        val credentialsActivitiesInContact1 = contactDao.getCredentialsActivityHistoriesByConnection("connection1")
        MatcherAssert.assertThat("Total of issued credentials activities in contact 1 is wrong", credentialsActivitiesInContact1.size == 2)
        // Check data for contact 2
        val contact2ByConnectionId = contactDao.getContactByConnectionId("connection2")!!
        MatcherAssert.assertThat("contact 2 was not saved correctly", contact2ByConnectionId.connectionId == "connection2" && contact2ByConnectionId.name == "Contact 2")
        val contact2Credentials = contactDao.getIssuedCredentials("connection2")
        MatcherAssert.assertThat("Total of credentials in contact 2 is wrong", contact2Credentials.size == 1)
        val contactAddedActivitiesForContact2 = contactDao.getActivityHistoriesByConnection("connection2").filter {
            it.activityHistory.type == ActivityHistory.Type.ContactAdded
        }
        MatcherAssert.assertThat("Total of contact added activities in contact 2 is wrong", contactAddedActivitiesForContact2.size == 1)
        val credentialsActivitiesInContact2 = contactDao.getCredentialsActivityHistoriesByConnection("connection2")
        MatcherAssert.assertThat("Total of issued credentials activities in contact 2 is wrong", credentialsActivitiesInContact2.size == 1)

        // Check Encoded credentials data
        val credentialWithEncodedCredential1 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential1")
        MatcherAssert.assertThat(
            "Encoded credential 1 is not saved correctly",
            credentialWithEncodedCredential1?.encodedCredential?.credentialEncoded == "encodedData1".toByteStringUTF8()
        )
        val credentialWithEncodedCredential2 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential2")
        MatcherAssert.assertThat(
            "Encoded credential 2 is not saved correctly",
            credentialWithEncodedCredential2?.encodedCredential?.credentialEncoded == "encodedData2".toByteStringUTF8()
        )
        val credentialWithEncodedCredential3 = credentialDao.getCredentialWithEncodedCredentialByCredentialId("credential3")
        MatcherAssert.assertThat(
            "Encoded credential 3 is not saved correctly",
            credentialWithEncodedCredential3?.encodedCredential?.credentialEncoded == "encodedData3".toByteStringUTF8()
        )
    }

    @Test(expected = InvalidObjectException::class)
    fun insertAllWithInvalidIssuedCredentials() = runBlocking {
        contactDao.removeAllData()
        val map: MutableMap<Contact, List<CredentialWithEncodedCredential>> = mutableMapOf()
        val contact1 = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection2", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        val credential2 = buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        map[contact1] = listOf(credential1, credential2)
        val contact2 = buildSimpleContact("connection2", "Contact 2")
        val credential3 = buildSimpleCredential("connection1", "Contact 2", "credential3", "encodedData3".toByteStringUTF8())
        map[contact2] = listOf(credential3)
        contactDao.insertAll(map)
    }

    @Test
    fun deleteContactWithIssuedCredentials() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        val credential2 = buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        val contactId = contactDao.insert(contact, listOf(credential1, credential2))

        // when a contact is deleted, all of their issued credentials should be deleted (only is a soft delete)
        contactDao.delete(contactDao.contactById(contactId.toInt())!!)

        val visibleContacts = contactDao.getAll()
        MatcherAssert.assertThat("there should not be a contact", visibleContacts.isEmpty())
        val deletedContacts = contactDao.getAllRemoved()
        MatcherAssert.assertThat("the contact should remain as deleted", deletedContacts.size == 1)
        val deletedContact = contactDao.getContactByConnectionId("connection1")
        MatcherAssert.assertThat("the contact should be as deleted", deletedContact!!.deleted == true)
        val credentials = contactDao.getIssuedCredentials("connection1")
        val deletedCredentials = contactDao.getDeletedIssuedCredentials("connection1")
        MatcherAssert.assertThat("The contact should have all their issued credentials as deleted", deletedCredentials.size == 2 && credentials.size == 0)
        val deletedContactActivitiesHistories = contactDao.getActivityHistoriesByConnection("connection1").filter {
            it.activityHistory.type == ActivityHistory.Type.ContactDeleted
        }
        MatcherAssert.assertThat("there should be a deleted contact activity history", deletedContactActivitiesHistories.size == 1)
    }

    @Test
    fun removeAllData() = runBlocking {
        contactDao.removeAllData()
        val contact = buildSimpleContact("connection1", "Contact 1")
        val credential1 = buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8())
        val credential2 = buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8())
        contactDao.insert(contact, listOf(credential1, credential2))

        // This is a permanent deletion in the contacts entity, and should also cascade the entire credentials entity and activityHistories.
        contactDao.removeAllData()

        val totalOfContacts = contactDao.getAll().size + contactDao.getAllRemoved().size
        MatcherAssert.assertThat("there should be no contacs", totalOfContacts == 0)
        val totalOfCredentials = credentialDao.getAllCredentials().size + credentialDao.getAllDeletedCredentials().size
        MatcherAssert.assertThat("there should be no credentials", totalOfCredentials == 0)
        val totalOfActivityHistories = contactDao.getAllActivityHistories().size
        MatcherAssert.assertThat("there should be no activity histories", totalOfActivityHistories == 0)
        val totalOfEncodedCredentials = credentialDao.totalOfEncodedCredentials()
        MatcherAssert.assertThat("there should be no Econded credentials", totalOfEncodedCredentials == 0)
    }

    private fun buildSimpleContact(connectionId: String, name: String): Contact {
        val contact = Contact()
        contact.connectionId = connectionId
        contact.name = name
        contact.dateCreated = Date().time
        return contact
    }

    private fun buildSimpleCredential(
        connectionId: String,
        issuerName: String,
        credentialId: String,
        encodedCredentialData: ByteString
    ): CredentialWithEncodedCredential {
        val credential = Credential()
        credential.issuerName = issuerName
        credential.credentialId = credentialId
        credential.connectionId = connectionId
        credential.dateReceived = Date().time
        val encodedCredential = EncodedCredential()
        encodedCredential.credentialEncoded = encodedCredentialData
        encodedCredential.credentialId = credentialId
        return CredentialWithEncodedCredential(credential, encodedCredential)
    }

    private fun String.toByteStringUTF8(): ByteString = ByteString.copyFrom(this, Charsets.UTF_8)
}
