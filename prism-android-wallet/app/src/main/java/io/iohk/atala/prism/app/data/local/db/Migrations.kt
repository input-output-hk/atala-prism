package io.iohk.atala.prism.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
* Added onDelete Cascade for credentials and "deleted" column in "credentials" and "contacts" table
* */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("DROP TABLE IF EXISTS credential_history")

        // UPDATE contact table
        database.execSQL("CREATE TABLE IF NOT EXISTS contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "connection_id TEXT, " +
                "date_created INTEGER, " +
                "did TEXT, " +
                "last_message_id TEXT, " +
                "name TEXT, " +
                "token TEXT, " +
                "key_derivation_path TEXT, " +
                "logo BLOB, " +
                "deleted INTEGER DEFAULT false)")


        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contacts_connection_id ON contacts (connection_id)")

        database.execSQL("""
                INSERT INTO contacts (id, connection_id, date_created, did, last_message_id, name, key_derivation_path, logo)
                SELECT id, connection_id, date_created, did, last_message_id, name, key_derivation_path, logo FROM contact
                """.trimIndent())

        database.execSQL("DROP TABLE contact")

        // Update credentials table

        database.execSQL(("CREATE TABLE IF NOT EXISTS credentials (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "credential_id TEXT, " +
                "date_received INTEGER, " +
                "credential_encoded BLOB, " +
                "html_view TEXT, " +
                "issuer_id TEXT, " +
                "issuer_name TEXT, " +
                "credential_type TEXT, " +
                "connection_id TEXT NOT NULL, " +
                "credentials_document TEXT, " +
                "deleted INTEGER DEFAULT false, " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"))

        database.execSQL("CREATE INDEX IF NOT EXISTS index_credentials_connection_id ON credentials (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credentials_credential_id ON credentials (credential_id)")

        database.execSQL("""
                INSERT INTO credentials (id, credential_id, date_received, credential_encoded, html_view, issuer_id, issuer_name, credential_type, connection_id, credentials_document)
                SELECT id, credential_id, date_received, credential_encoded, html_view, issuer_id, issuer_name, credential_type, connection_id, credentials_document FROM credential
                """.trimIndent())

        database.execSQL("DROP TABLE credential")
    }
}

/*
* Create "activityHistories" table
* */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Create activityHistories table
        database.execSQL("CREATE TABLE IF NOT EXISTS activityHistories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT, " +
                "credential_id TEXT, " +
                "date INTEGER NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "FOREIGN KEY(credential_id) REFERENCES credentials(credential_id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )")


        database.execSQL("CREATE INDEX IF NOT EXISTS index_activityHistories_credential_id ON activityHistories (credential_id)")

        database.execSQL("CREATE INDEX IF NOT EXISTS index_activityHistories_connection_id ON activityHistories (connection_id)")
    }
}

/*
* Added "needs_to_be_notified" to activityHistories table
* */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE activityHistories " +
                "ADD COLUMN needs_to_be_notified INTEGER NOT NULL DEFAULT false")
    }
}

/*
* Added proofRequests and proofRequestCredential tables
* */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // proofRequests
        database.execSQL("CREATE TABLE IF NOT EXISTS proofRequests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT NOT NULL, " +
                "message_id TEXT NOT NULL, " +
                "FOREIGN KEY(connection_id) " +
                "REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )")

        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequests_connection_id ON proofRequests (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_proofRequests_message_id ON proofRequests (message_id)")

        //proofRequestCredential relation
        database.execSQL("CREATE TABLE IF NOT EXISTS proofRequestCredential (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "credential_id TEXT NOT NULL, " +
                "proof_request_id INTEGER NOT NULL, " +
                "FOREIGN KEY(credential_id) " +
                "REFERENCES credentials(credential_id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(proof_request_id) REFERENCES proofRequests(id) ON UPDATE NO ACTION ON DELETE CASCADE )")

        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequestCredential_credential_id ON proofRequestCredential (credential_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequestCredential_proof_request_id ON proofRequestCredential (proof_request_id)")
    }
}