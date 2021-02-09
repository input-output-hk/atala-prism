package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ProofRequestWithContactAndCredentials(
        @Embedded val proofRequest: ProofRequest,
        @Relation(
                parentColumn = "connection_id",
                entity = Contact::class,
                entityColumn = "connection_id"
        ) var contact: Contact?,
        @Relation(
                parentColumn = "id",
                entity = Credential::class,
                entityColumn = "credential_id",
                associateBy = Junction(
                        value = ProofRequestCredential::class,
                        parentColumn = "proof_request_id",
                        entityColumn = "credential_id"
                )
        ) var credentials: List<Credential>
)