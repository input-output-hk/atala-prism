package io.iohk.atala.prism.app

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import io.iohk.atala.prism.app.data.local.db.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.IOException

@RunWith(BlockJUnit4ClassRunner::class)
class MigrationsTest {

    private val TEST_DB_NAME = "migration-test"

    // Array of all migrations
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

    private lateinit var helper: MigrationTestHelper

    @Before
    fun buildHelper() {
        helper = MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                AppDatabase::class.java.canonicalName,
                FrameworkSQLiteOpenHelperFactory())
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {

        // Create earliest version of the database.
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
                AppDatabase::class.java,
                TEST_DB_NAME).addMigrations(*ALL_MIGRATIONS)
                .build().apply {
                    openHelper.writableDatabase.close()
                }
    }
}