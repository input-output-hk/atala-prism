package io.iohk.atala.mirror

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, MirrorMessage, RegisterAddressMessage}
import io.iohk.atala.mirror.db.CardanoAddressInfoDao
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.mirror.models.UserCredential._
import io.iohk.atala.mirror.models._
import io.iohk.atala.prism.credentials.{
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  SignedCredential,
  SlayerCredentialId,
  TimestampInfo,
  UnsignedCredential
}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair}
import io.iohk.atala.mirror.models.CardanoAddressInfo.{CardanoAddress, RegistrationDate}
import io.iohk.atala.mirror.stubs.NodeClientServiceStub
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.Credential
import io.iohk.atala.prism.protos.node_api.GetCredentialStateResponse
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.EcKeyData
import io.iohk.atala.prism.protos.node_models.{DIDData, KeyUsage, PublicKey}

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
        ConnectorMessageId("messageId1"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )

    lazy val userCredential2: UserCredential =
      UserCredential(
        ConnectionFixtures.connection2.token,
        RawCredential("rawCredentials2"),
        None,
        ConnectorMessageId("messageId2"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )
  }

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = "did:prism:123456678abcdefg"

    val unsignedCredential: UnsignedCredential = createUnsignedCredential(issuanceKeyId, issuerDID)

    val keyAddedDate: TimestampInfo = TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
    val credentialIssueDate: TimestampInfo = TimestampInfo(Instant.now(), 2, 2)

    val keys: ECKeyPair = EC.generateKeyPair()
    val signedCredential: SignedCredential = createSignedCredential(unsignedCredential, keys)

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

    val nodeCredentialId: SlayerCredentialId = SlayerCredentialId
      .compute(
        credential = signedCredential,
        did = issuerDID
      )

    val defaultNodeClientStub =
      new NodeClientServiceStub(Map(issuerDID -> didData), Map(nodeCredentialId.string -> getCredentialStateResponse))

    val rawMessage: ByteString = createRawMessage("{}")

    def createRawMessage(json: String): ByteString = {
      Credential(typeId = "VerifiableCredential/RedlandIdCredential", credentialDocument = json).toByteString
    }

    def createUnsignedCredential(issuanceKeyId: String, issuerDID: String): UnsignedCredential =
      JsonBasedUnsignedCredential.jsonBasedUnsignedCredential.buildFrom(
        issuerDID = issuerDID,
        issuanceKeyId = issuanceKeyId,
        claims = Json.obj()
      )

    def createSignedCredential(unsignedCredential: UnsignedCredential, keys: ECKeyPair): SignedCredential =
      CredentialsCryptoSDKImpl.signCredential(unsignedCredential, keys.privateKey)(EC)

  }

  object CardanoAddressInfoFixtures {
    import ConnectionFixtures._

    lazy val cardanoAddressInfo1 = CardanoAddressInfo(
      cardanoAddress = CardanoAddress("address1"),
      connectionToken = connection1.token,
      registrationDate = RegistrationDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
      messageId = ConnectorMessageId("messageId1")
    )

    lazy val cardanoAddressInfo2 = CardanoAddressInfo(
      cardanoAddress = CardanoAddress("address2"),
      connectionToken = connection2.token,
      registrationDate = RegistrationDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
      messageId = ConnectorMessageId("messageId2")
    )

    def insertAll[F[_]: Sync](database: Transactor[F]): F[Unit] = {
      insertManyFixtures(
        CardanoAddressInfoDao.insert(cardanoAddressInfo1),
        CardanoAddressInfoDao.insert(cardanoAddressInfo2)
      )(database)
    }
  }

  object ConnectorMessageFixtures {
    import ConnectionFixtures._
    import CredentialFixtures._

    lazy val credentialMessage1 = ReceivedMessage(
      id = "id1",
      received = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
      connectionId = connectionId1.uuid.toString,
      message = Credential(
        typeId = "VerifiableCredential/RedlandIdCredential",
        credentialDocument = signedCredential.canonicalForm
      ).toByteString
    )

    lazy val credentialMessage2 = ReceivedMessage(
      id = "id2",
      received = LocalDateTime.of(2020, 6, 14, 0, 0).toEpochSecond(ZoneOffset.UTC),
      connectionId = connectionId2.uuid.toString,
      message = Credential(
        typeId = "VerifiableCredential/RedlandIdCredential",
        credentialDocument = signedCredential.canonicalForm
      ).toByteString
    )

    val cardanoAddress1 = "cardanoAddress1"

    lazy val cardanoAddressInfoMessage1 = ReceivedMessage(
      id = "id3",
      received = LocalDateTime.of(2020, 6, 13, 0, 0).toEpochSecond(ZoneOffset.UTC),
      connectionId = connectionId1.uuid.toString,
      message = AtalaMessage()
        .withMirrorMessage(MirrorMessage().withRegisterAddressMessage(RegisterAddressMessage(cardanoAddress1)))
        .toByteString
    )
  }
}
