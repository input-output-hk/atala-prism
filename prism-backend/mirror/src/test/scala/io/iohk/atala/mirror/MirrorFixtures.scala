package io.iohk.atala.mirror

import java.time.{LocalDateTime, ZoneOffset}
import cats.effect.Sync
import cats.implicits._
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, MirrorMessage, RegisterAddressMessage}
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao, UserCredentialDao}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.mirror.models.UserCredential._
import io.iohk.atala.mirror.models._
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.mirror.models.CardanoAddressInfo.{CardanoNetwork, RegistrationDate}
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.circe.syntax._
import io.iohk.atala.mirror.config.{HttpConfig, MirrorConfig, TrisaConfig}
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.jose.implicits._
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken, ConnectorMessageId}
import io.iohk.atala.prism.utils.GrpcUtils.{GrpcConfig, SslConfig}
import io.iohk.atala.prism.services.ServicesFixtures

trait MirrorFixtures extends ServicesFixtures {

  private implicit def ec = EC

  lazy val mirrorConfig: MirrorConfig = MirrorConfig(
    GrpcConfig(50057),
    HttpConfig(8080, "localhost"),
    TrisaConfig(
      enabled = false,
      GrpcConfig(7777),
      SslConfig("mirror/etc/trisa/server.crt", "mirror/etc/trisa/server.key", "mirror/etc/trisa/trust.chain")
    )
  )

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
    lazy val connectionId1: ConnectionId = ConnectionId.unsafeFrom("3a66fcef-4d50-4a67-a365-d4dbebcf22d3")
    lazy val connectionId2: ConnectionId = ConnectionId.unsafeFrom("06325aef-d937-41b2-9a6c-b654e02b273d")
    lazy val connectionPayIdName2 = PayIdName("payIdName2")
    lazy val connectionHolderDid2 = newDID()
    lazy val connection1: Connection =
      Connection(
        token = ConnectionToken("token1"),
        id = Some(connectionId1),
        state = ConnectionState.Invited,
        updatedAt = LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC),
        holderDID = None,
        payIdName = None
      )
    lazy val connection2: Connection =
      Connection(
        token = ConnectionToken("token2"),
        id = Some(connectionId2),
        state = ConnectionState.Invited,
        updatedAt = LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC),
        holderDID = Some(connectionHolderDid2),
        payIdName = Some(connectionPayIdName2)
      )

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
        RawCredential(CredentialFixtures.jsonBasedCredential1.canonicalForm),
        Some(newDID()),
        ConnectorMessageId("messageId1"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )

    lazy val userCredential2: UserCredential =
      UserCredential(
        ConnectionFixtures.connection1.token,
        RawCredential(CredentialFixtures.jsonBasedCredential2.canonicalForm),
        Some(newDID()),
        ConnectorMessageId("messageId2"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )

    lazy val userCredential3: UserCredential =
      UserCredential(
        ConnectionFixtures.connection2.token,
        RawCredential(CredentialFixtures.jsonBasedCredential1.canonicalForm),
        None,
        ConnectorMessageId("messageId3"),
        MessageReceivedDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
        CredentialStatus.Valid
      )

    def insertAll[F[_]: Sync](database: Transactor[F]) = {
      insertManyFixtures(
        UserCredentialDao.insert(userCredential1),
        UserCredentialDao.insert(userCredential2),
        UserCredentialDao.insert(userCredential3)
      )(database)
    }
  }

  object CardanoAddressInfoFixtures {
    import ConnectionFixtures._, CredentialFixtures._

    lazy val cardanoNetwork1 = CardanoNetwork("mainnet")
    lazy val cardanoNetwork2 = CardanoNetwork("testnet")

    lazy val cardanoAddressInfo1 = CardanoAddressInfo(
      cardanoAddress = CardanoAddress("address1"),
      payidVerifiedAddress = None,
      cardanoNetwork = cardanoNetwork1,
      connectionToken = connection1.token,
      registrationDate = RegistrationDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
      messageId = ConnectorMessageId("messageId1")
    )

    lazy val payId1 = PayID(connectionHolderDid2.value + "$" + mirrorConfig.httpConfig.payIdHostAddress)
    lazy val cardanoAddressInfo2Address = "address2"
    lazy val cardanoAddressInfo2 = CardanoAddressInfo(
      cardanoAddress = CardanoAddress(cardanoAddressInfo2Address),
      payidVerifiedAddress = Some(
        VerifiedAddress(
          VerifiedAddressWrapper(
            payId = payId1,
            payIdAddress = Address(
              paymentNetwork = "cardano",
              environment = Some("testnet"),
              addressDetails = CryptoAddressDetails(
                address = cardanoAddressInfo2Address,
                tag = None
              )
            )
          ),
          keys,
          keyId = s"$issuerDID#$issuanceKeyId"
        )
      ),
      cardanoNetwork = cardanoNetwork2,
      connectionToken = connection2.token,
      registrationDate = RegistrationDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC)),
      messageId = ConnectorMessageId("messageId2")
    )

    lazy val cardanoAddressInfo3 = CardanoAddressInfo(
      cardanoAddress = CardanoAddress("address3"),
      payidVerifiedAddress = None,
      cardanoNetwork = cardanoNetwork2,
      connectionToken = connection2.token,
      registrationDate = RegistrationDate(LocalDateTime.of(2020, 10, 6, 0, 0).toInstant(ZoneOffset.UTC)),
      messageId = ConnectorMessageId("messageId3")
    )

    def insertAll[F[_]: Sync](database: Transactor[F]): F[Unit] = {
      insertManyFixtures(
        CardanoAddressInfoDao.insert(cardanoAddressInfo1),
        CardanoAddressInfoDao.insert(cardanoAddressInfo2),
        CardanoAddressInfoDao.insert(cardanoAddressInfo3)
      )(database)
    }
  }

  object ConnectorMessageFixtures {
    import ConnectionFixtures._
    import CredentialFixtures._

    lazy val credentialMessage1 = ReceivedMessage(
      id = "id1",
      receivedDeprecated = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
      received = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = plainTextCredentialMessage(jsonBasedCredential1, proof1).toByteString
    )

    lazy val credentialMessage2 = ReceivedMessage(
      id = "id2",
      receivedDeprecated = LocalDateTime.of(2020, 6, 14, 0, 0).toEpochSecond(ZoneOffset.UTC),
      received = Timestamp(LocalDateTime.of(2020, 6, 14, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId2.uuid.toString,
      message = plainTextCredentialMessage(jsonBasedCredential2, proof2).toByteString
    )

    val cardanoAddress1 = "cardanoAddress1"

    lazy val cardanoAddressInfoMessage1 = ReceivedMessage(
      id = "id3",
      receivedDeprecated = LocalDateTime.of(2020, 6, 13, 0, 0).toEpochSecond(ZoneOffset.UTC),
      received = Timestamp(LocalDateTime.of(2020, 6, 13, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = AtalaMessage()
        .withMirrorMessage(MirrorMessage().withRegisterAddressMessage(RegisterAddressMessage(cardanoAddress1)))
        .toByteString
    )
  }

  object PayIdFixtures {
    import ConnectionFixtures._, CredentialFixtures._

    val cardanoAddressPayId1 = "cardanoAddressPayId1"

    lazy val payId1 = PayID(connectionHolderDid2.value + "$" + mirrorConfig.httpConfig.payIdHostAddress)
    lazy val paymentInformation1 = PaymentInformation(
      payId = Some(payId1),
      version = None,
      addresses = List.empty,
      verifiedAddresses = List(
        VerifiedAddress(
          VerifiedAddressWrapper(
            payId = payId1,
            payIdAddress = Address(
              paymentNetwork = "cardano",
              environment = Some("testnet"),
              addressDetails = CryptoAddressDetails(
                address = cardanoAddressPayId1,
                tag = None
              )
            )
          ),
          keys,
          keyId = s"${issuerDID.value}#$issuanceKeyId"
        )
      ),
      memo = None
    )

    lazy val paymentInformationMessage1 = ReceivedMessage(
      id = "id1",
      receivedDeprecated = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
      received = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId2.uuid.toString,
      message = paymentInformationToAtalaMessage(paymentInformation1)
    )

    def paymentInformationToAtalaMessage(paymentInformation: PaymentInformation): ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage().withPayIdMessage(credential_models.PayIdMessage(paymentInformation.asJson.toString()))
        )
        .toByteString

    lazy val payIdName1 = "newPayIdName1"

    lazy val payIdNameRegistrationMessage1 = ReceivedMessage(
      id = "id1",
      receivedDeprecated = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
      received = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = payIdNameRegistrationToAtalaMessage(payIdName1)
    )

    def payIdNameRegistrationToAtalaMessage(payIdName: String): ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage().withPayIdNameRegistrationMessage(credential_models.PayIdNameRegistrationMessage(payIdName))
        )
        .toByteString
  }
}
