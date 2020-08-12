package cvp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.iohk.cvp.data.local.db.AppDatabase
import io.iohk.cvp.data.local.db.dao.ContactDao
import io.iohk.cvp.data.local.db.model.Contact
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
class ContactTableTest {
    private lateinit var contactDao: ContactDao
    private lateinit var db: AppDatabase

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
                context, AppDatabase::class.java).setTransactionExecutor(testDispatcher.asExecutor())
                .setQueryExecutor(testDispatcher.asExecutor()).build()
        contactDao = db.contactDao()
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
            val connectionId = "1"
            val connection = Contact()

            connection.connectionId = connectionId
            contactDao.insert(connection)
            val byId = contactDao.getContactByConnectionId(connectionId)
            MatcherAssert.assertThat(byId?.connectionId, equalTo(connection.connectionId))
        }
    }

}