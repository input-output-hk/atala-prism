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
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "connection_id TEXT, " +
                "date_created INTEGER, " +
                "did TEXT, " +
                "last_message_id TEXT, " +
                "name TEXT, " +
                "token TEXT, " +
                "key_derivation_path TEXT, " +
                "logo BLOB, " +
                "deleted INTEGER DEFAULT false)"
        )

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contacts_connection_id ON contacts (connection_id)")

        database.execSQL(
            """
                INSERT INTO contacts (id, connection_id, date_created, did, last_message_id, name, key_derivation_path, logo)
                SELECT id, connection_id, date_created, did, last_message_id, name, key_derivation_path, logo FROM contact
            """.trimIndent()
        )

        database.execSQL("DROP TABLE contact")

        // Update credentials table

        database.execSQL(
            (
                "CREATE TABLE IF NOT EXISTS credentials (" +
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
                    "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_credentials_connection_id ON credentials (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credentials_credential_id ON credentials (credential_id)")

        database.execSQL(
            """
                INSERT INTO credentials (id, credential_id, date_received, credential_encoded, html_view, issuer_id, issuer_name, credential_type, connection_id, credentials_document)
                SELECT id, credential_id, date_received, credential_encoded, html_view, issuer_id, issuer_name, credential_type, connection_id, credentials_document FROM credential
            """.trimIndent()
        )

        database.execSQL("DROP TABLE credential")
    }
}

/*
* Create "activityHistories" table
* */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Create activityHistories table
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS activityHistories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT, " +
                "credential_id TEXT, " +
                "date INTEGER NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "FOREIGN KEY(credential_id) REFERENCES credentials(credential_id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_activityHistories_credential_id ON activityHistories (credential_id)")

        database.execSQL("CREATE INDEX IF NOT EXISTS index_activityHistories_connection_id ON activityHistories (connection_id)")
    }
}

/*
* Added "needs_to_be_notified" to activityHistories table
* */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE activityHistories " +
                "ADD COLUMN needs_to_be_notified INTEGER NOT NULL DEFAULT false"
        )
    }
}

/*
* Added proofRequests and proofRequestCredential tables
* */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // proofRequests
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS proofRequests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT NOT NULL, " +
                "message_id TEXT NOT NULL, " +
                "FOREIGN KEY(connection_id) " +
                "REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequests_connection_id ON proofRequests (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_proofRequests_message_id ON proofRequests (message_id)")

        // proofRequestCredential relation
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS proofRequestCredential (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "credential_id TEXT NOT NULL, " +
                "proof_request_id INTEGER NOT NULL, " +
                "FOREIGN KEY(credential_id) " +
                "REFERENCES credentials(credential_id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(proof_request_id) REFERENCES proofRequests(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequestCredential_credential_id ON proofRequestCredential (credential_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_proofRequestCredential_proof_request_id ON proofRequestCredential (proof_request_id)")
    }
}

/*
* Added encodedCredentials table and removed "credential_encoded", "html_view" and "credentials_document" fields from "credentials" table
* */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Added encodedCredentials table
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS encodedCredentials (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "credential_id TEXT, " +
                "credential_encoded BLOB, " +
                "FOREIGN KEY(credential_id) " +
                "REFERENCES credentials(credential_id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_encodedCredentials_credential_id ON encodedCredentials (credential_id)")

        // Insert current encoded data from "credentials" table to "encodedCredentials" table
        database.execSQL(
            """
                INSERT INTO encodedCredentials (credential_id, credential_encoded)
                SELECT credential_id, credential_encoded FROM credentials
            """.trimIndent()
        )
        /*
        * Removed "credential_encoded", "html_view" and "credentials_document" fields from "credentials" table.
        * We must recreate the "credentials" table due to limitations of SQlite info: https://www.sqlite.org/faq.html#q11
        * */
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS credentials_backup (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "credential_id TEXT, " +
                "date_received INTEGER, " +
                "issuer_id TEXT, " +
                "issuer_name TEXT, " +
                "credential_type TEXT, " +
                "connection_id TEXT NOT NULL, " +
                "deleted INTEGER DEFAULT false, " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
            """
                INSERT INTO credentials_backup (id, credential_id, date_received, issuer_id, issuer_name, credential_type, connection_id, deleted)
                SELECT id, credential_id, date_received, issuer_id, issuer_name, credential_type, connection_id, deleted FROM credentials
            """.trimIndent()
        )

        database.execSQL("DROP TABLE credentials")
        database.execSQL("ALTER TABLE credentials_backup RENAME TO credentials")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_credentials_connection_id ON credentials (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credentials_credential_id ON credentials (credential_id)")
    }
}

/*
* Added payIds and payIdAddresses tables
* */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // payIds
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS payIds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT NOT NULL, name TEXT, " +
                "FOREIGN KEY(connection_id) " +
                "REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIds_connection_id ON payIds (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIds_name ON payIds (name)")

        // payIdAddresses
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS payIdAddresses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "pay_id_local_id INTEGER NOT NULL, " +
                "address TEXT NOT NULL, " +
                "message_id TEXT NOT NULL, " +
                "FOREIGN KEY(pay_id_local_id) REFERENCES payIds(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_payIdAddresses_pay_id_local_id ON payIdAddresses (pay_id_local_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIdAddresses_address ON payIdAddresses (address)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        /*
        * Added "message_id", and "status" fields to "payIds" table.
        * We must recreate the "payIds" table due to limitations of SQlite info: https://www.sqlite.org/faq.html#q11
        * */
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS payIds_backup (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT NOT NULL, " +
                "name TEXT, " +
                "message_id TEXT, " +
                "status INTEGER NOT NULL, " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
            """
                INSERT INTO payIds_backup (id, connection_id, name, status)
                SELECT id, connection_id, name, 1 FROM payIds
            """.trimIndent()
        )

        database.execSQL("DROP TABLE payIds")
        database.execSQL("ALTER TABLE payIds_backup RENAME TO payIds")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIds_connection_id ON payIds (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIds_name ON payIds (name)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIds_message_id ON payIds (message_id)")
    }
}

/*
* Create "kycRequests" table
* */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS kycRequests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "connection_id TEXT NOT NULL, " +
                "message_id TEXT NOT NULL, " +
                "bearer_token TEXT NOT NULL, " +
                "instance_id TEXT NOT NULL, " +
                "FOREIGN KEY(connection_id) REFERENCES contacts(connection_id) ON UPDATE NO ACTION ON DELETE CASCADE " +
                ")"
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_kycRequests_connection_id ON kycRequests (connection_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_kycRequests_message_id ON kycRequests (message_id)")
    }
}

/*
* Added payIdPublicKeys table
* */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE IF NOT EXISTS payIdPublicKeys (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "pay_id_local_id INTEGER NOT NULL, " +
                "public_key TEXT NOT NULL, " +
                "message_id TEXT NOT NULL, " +
                "FOREIGN KEY(pay_id_local_id) REFERENCES payIds(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_payIdPublicKeys_pay_id_local_id ON payIdPublicKeys (pay_id_local_id)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payIdPublicKeys_public_key ON payIdPublicKeys (public_key)")
    }
}
