package io.iohk.atala.prism.app.data.local.db.converters

import androidx.room.TypeConverter
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory

class ActivityHistoryTypeConverter {
    @TypeConverter
    fun fromIntToActivityHistoryType(value: Int): ActivityHistory.Type? {
        return when (value) {
            ActivityHistory.Type.ContactAdded.ordinal -> ActivityHistory.Type.ContactAdded
            ActivityHistory.Type.ContactDeleted.ordinal -> ActivityHistory.Type.ContactDeleted
            ActivityHistory.Type.CredentialDeleted.ordinal -> ActivityHistory.Type.CredentialDeleted
            ActivityHistory.Type.CredentialIssued.ordinal -> ActivityHistory.Type.CredentialIssued
            ActivityHistory.Type.CredentialShared.ordinal -> ActivityHistory.Type.CredentialShared
            ActivityHistory.Type.CredentialRequested.ordinal -> ActivityHistory.Type.CredentialRequested
            else -> null
        }
    }

    @TypeConverter
    fun fromCActivityHistoryTypeToInt(value: ActivityHistory.Type): Int {
        return value.ordinal
    }
}