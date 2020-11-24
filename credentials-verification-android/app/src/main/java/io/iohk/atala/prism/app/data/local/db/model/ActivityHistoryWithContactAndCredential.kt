package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class ActivityHistoryWithContactAndCredential(
        @Embedded val activityHistory: ActivityHistory,
        @Relation(
                entity = Credential::class,
                entityColumn = "credential_id",
                parentColumn = "credential_id"
        ) val credential: Credential?,
        @Relation(
                entity = Contact::class,
                entityColumn = "connection_id",
                parentColumn = "connection_id"
        ) val contact: Contact?
)