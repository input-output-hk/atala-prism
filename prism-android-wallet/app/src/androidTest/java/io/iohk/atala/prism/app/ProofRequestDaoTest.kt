package io.iohk.atala.prism.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.base.Charsets
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.local.db.AppDatabase
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.IOException
import java.util.Date
import java.util.concurrent.Executors

@RunWith(BlockJUnit4ClassRunner::class)
class ProofRequestDaoTest {
    private lateinit var contactDao: ContactDao

    private lateinit var credentialDao: CredentialDao

    private lateinit var proofRequestDao: ProofRequestDao

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
        proofRequestDao = db.proofRequestDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Test for [ProofRequestDao.insertAllSync] and [ProofRequestDao.getAllWithCredentials]
     * */
    @Test
    @Throws(Exception::class)
    fun insertAllAndGetAll() = runBlocking {
        contactDao.insert(
            buildSimpleContact("connection1", "Contact 1"),
            listOf(
                buildSimpleCredential("connection1", "Contact 1", "credential1", "encodedData1".toByteStringUTF8()),
                buildSimpleCredential("connection1", "Contact 1", "credential2", "encodedData2".toByteStringUTF8()),
                buildSimpleCredential("connection1", "Contact 1", "credential3", "encodedData3".toByteStringUTF8())
            )
        )
        contactDao.insert(buildSimpleContact("connection2", "Contact 2"))
        val credentials = contactDao.getIssuedCredentials("connection1")

        proofRequestDao.insertAllSync(
            listOf(
                Pair(
                    ProofRequest("connection2", "message1"),
                    listOf(
                        credentials[0],
                        credentials[1]
                    )
                )
            )
        )
        val proofRequests = proofRequestDao.getAllWithCredentials()

        MatcherAssert.assertThat("Total of proof requests is wrong", proofRequests.size == 1)
        MatcherAssert.assertThat("The requesting connection id is wrong", proofRequests[0].proofRequest.connectionId == "connection2")
        MatcherAssert.assertThat("The total of credentials for the proof request are incorrect", proofRequests[0].credentials.size == 2)
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
