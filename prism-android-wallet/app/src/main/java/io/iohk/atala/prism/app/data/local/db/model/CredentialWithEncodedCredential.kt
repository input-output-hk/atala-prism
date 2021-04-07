package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class CredentialWithEncodedCredential(
    @Embedded val credential: Credential,
    @Relation(
        entity = EncodedCredential::class,
        entityColumn = "credential_id",
        parentColumn = "credential_id"
    ) val encodedCredential: EncodedCredential
)
