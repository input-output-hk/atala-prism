package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class ActivityHistoryWithContact(
    @Embedded val activityHistory: ActivityHistory,
    @Relation(
        entity = Contact::class,
        entityColumn = "connection_id",
        parentColumn = "connection_id"
    ) val contact: Contact?
)
