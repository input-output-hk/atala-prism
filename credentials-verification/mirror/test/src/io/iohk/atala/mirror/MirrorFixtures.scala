package io.iohk.atala.mirror

import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset}

import cats.effect.Sync
import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO
import com.google.protobuf.ByteString
import io.iohk.prism.protos.credential_models.Credential
import io.iohk.atala.mirror.models._
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.mirror.models.UserCredential._
import io.iohk.atala.mirror.db.ConnectionDao
import doobie.implicits._
import cats.implicits._
import io.circe.Json
import io.iohk.atala.credentials.{
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  SignedCredential,
  TimestampInfo,
  UnsignedCredential
}
import io.iohk.atala.crypto.{EC, ECKeyPair}
import io.iohk.atala.mirror.NodeUtils.computeNodeCredentialId
import io.iohk.atala.mirror.stubs.NodeClientServiceStub
import io.iohk.prism.protos.node_api.GetCredentialStateResponse
import io.iohk.prism.protos.node_models.{DIDData, KeyUsage, PublicKey}
import io.iohk.prism.protos.node_models.PublicKey.KeyData.EcKeyData

trait MirrorFixtures {

  /**
    * Helper method to insert multiple records into the database.
    *
    * example:
    * {{{
    *   insertManyFixtures(
    *     SomeDao.insert(record1),
    *     SomeDao.insert(record2)
    *   )(database).unsafeRunSync()
    * }}}
    */
  def insertManyFixtures[F[_]: Sync, M](records: ConnectionIO[M]*)(database: Transactor[F]): F[Unit] =
    records.toList.sequence.transact(database).void

  object ConnectionFixtures {
    lazy val connectionId1: ConnectionId = ConnectionId(UUID.fromString("0a66fcef-4d50-4a67-a365-d4dbebcf22d3"))
    lazy val connectionId2: ConnectionId = ConnectionId(UUID.fromString("36325aef-d937-41b2-9a6c-b654e02b273d"))
    lazy val connection1: Connection =
      Connection(ConnectionToken("token1"), Some(connectionId1), ConnectionState.Invited)
    lazy val connection2: Connection =
      Connection(ConnectionToken("token2"), Some(connectionId2), ConnectionState.Invited)

    def insertAll[F[_]: Sync](database: Transactor[F]) = {
      insertManyFixtures(
        ConnectionDao.insert(connection1),
        ConnectionDao.insert(connection2)
      )(database)
    }
  }

  object UserCredentialFixtures {
    lazy val userCredential1: UserCredential =
      UserCredential(
        ConnectionFixtures.connection1.token,
        RawCredential("rawCredentials1"),
        Some(IssuersDID("issuersDID1")),
        MessageId("messageId1"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )

    lazy val userCredential2: UserCredential =
      UserCredential(
        ConnectionFixtures.connection2.token,
        RawCredential("rawCredentials2"),
        None,
        MessageId("messageId2"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )
  }

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = "did:prism:123456678abcdefg"

    val unsignedCredential: UnsignedCredential = JsonBasedUnsignedCredential.jsonBasedUnsignedCredential.buildFrom(
      issuerDID = issuerDID,
      issuanceKeyId = issuanceKeyId,
      claims = Json.obj()
    )

    val keyAddedDate: TimestampInfo = TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
    val credentialIssueDate: TimestampInfo = TimestampInfo(Instant.now(), 2, 2)

    val keys: ECKeyPair = EC.generateKeyPair()
    val signedCredential: SignedCredential =
      CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)(EC)

    val publicKey: PublicKey = PublicKey(
      id = issuanceKeyId,
      usage = KeyUsage.AUTHENTICATION_KEY,
      addedOn = Some(NodeUtils.toInfoProto(keyAddedDate)),
      revokedOn = None,
      keyData = EcKeyData(NodeUtils.toTimestampInfoProto(keys.publicKey))
    )

    val didData: DIDData = DIDData("", Seq(publicKey))
    val getCredentialStateResponse: GetCredentialStateResponse =
      GetCredentialStateResponse(
        issuerDID = unsignedCredential.issuerDID.get,
        publicationDate = Some(NodeUtils.toInfoProto(credentialIssueDate)),
        revocationDate = None
      )

    val nodeCredentialId: String = computeNodeCredentialId(
      credentialHash = CredentialsCryptoSDKImpl.hash(signedCredential),
      did = issuerDID
    )

    val defaultNodeClientStub =
      new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId -> getCredentialStateResponse))

    val rawMessage: ByteString = createRawMessage("{}")

    def createRawMessage(json: String): ByteString = {
      Credential(typeId = "VerifiableCredential/RedlandIdCredential", credentialDocument = json).toByteString
    }

  }

}
