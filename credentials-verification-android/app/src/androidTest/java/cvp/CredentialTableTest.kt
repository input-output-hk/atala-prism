package cvp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.iohk.cvp.data.local.db.AppDatabase
import io.iohk.cvp.data.local.db.dao.CredentialDao
import io.iohk.cvp.data.local.db.model.Credential
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.IOException

@RunWith(BlockJUnit4ClassRunner::class)
class CredentialTableTest {
    private lateinit var credentialDao: CredentialDao
    private lateinit var db: AppDatabase

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
                context, AppDatabase::class.java).setTransactionExecutor(testDispatcher.asExecutor())
                .setQueryExecutor(testDispatcher.asExecutor()).build()
        credentialDao = db.credentialDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testContactTable_insertAndSelect() {
        testScope.runBlockingTest {
            val credentialId = "1"
            val credential = Credential()

            credential.credentialId = credentialId
            credentialDao.insert(credential)
            val byId = credentialDao.getCredentialByCredentialId(credentialId)
            MatcherAssert.assertThat(byId?.connectionId, equalTo(credential.connectionId))
        }
    }

}