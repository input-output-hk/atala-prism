package io.iohk.atala.mirror

import java.time.{LocalDateTime, ZoneOffset}

import cats.effect.Sync
import cats.implicits._
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import io.circe.syntax._
import org.scalatest.OptionValues._

import io.iohk.atala.mirror.config.{CardanoConfig, HttpConfig, MirrorConfig, TrisaConfig}
import io.iohk.atala.mirror.db._
import io.iohk.atala.mirror.models.CardanoAddressInfo.{CardanoNetwork, RegistrationDate}
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.mirror.models.UserCredential._
import io.iohk.atala.mirror.models._
import io.iohk.atala.mirror.services.CardanoAddressService
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.jose.implicits._
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken, ConnectorMessageId}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.{
  AtalaMessage,
  InitiateTrisaCardanoTransactionMessage,
  MirrorMessage,
  RegisterAddressMessage
}
import io.iohk.atala.prism.services.ServicesFixtures
import io.iohk.atala.prism.task.lease.system.ProcessingTaskFixtures
import io.iohk.atala.prism.utils.GrpcUtils.{GrpcConfig, SslConfig}
import io.iohk.atala.mirror.stubs.CardanoAddressServiceStub
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._

trait MirrorFixtures extends ServicesFixtures with ProcessingTaskFixtures {

  private implicit def ec = EC

  lazy val mirrorConfig: MirrorConfig = MirrorConfig(
    GrpcConfig(50057),
    HttpConfig(8080, "localhost"),
    TrisaConfig(
      enabled = false,
      GrpcConfig(7777),
      SslConfig("mirror/etc/trisa/server.crt", "mirror/etc/trisa/server.key", "mirror/etc/trisa/trust.chain")
    ),
    taskLeaseConfig
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

    val kycCredentialContent = CredentialContent(
      CredentialContent.JsonFields.CredentialType.field -> "KYCCredential",
      CredentialContent.JsonFields.CredentialSubject.field -> CredentialContent.Fields(
        "name" -> "Jo Wong",
        "birthDate" -> "1999-01-11",
        "idDocument" -> CredentialContent.Fields(
          "personalNumber" -> "RL-F95B27EAD"
        )
      )
    )

    val redlandIdCredentialContent = CredentialContent(
      CredentialContent.JsonFields.CredentialType.field -> CredentialContent
        .Values("VerifiableCredential", "RedlandIdCredential"),
      CredentialContent.JsonFields.CredentialSubject.field ->
        """{"id":"unknown","identityNumber":"RL-F95B27EAD","name":"Jo Wong","dateOfBirth":"1999-01-11"}"""
    )

    val unknownCredentialContent = CredentialContent(
      "unknown" -> "unknown"
    )

    def insertAll[F[_]: Sync](database: Transactor[F]) = {
      insertManyFixtures(
        UserCredentialDao.insert(userCredential1),
        UserCredentialDao.insert(userCredential2),
        UserCredentialDao.insert(userCredential3)
      )(database)
    }
  }

  object CardanoAddressFixtures {
    // extended public key was generated as:
    // echo "root_xsk12qaq7ccdh97uhvqxfn8l9f5ph0hcpf38nkv3yfpky8uge58gx9xr7up7havl0k8dgy0u38qzakhvmuwtzat4jnvyrjqyvwe2mpewemn6z0wajkjsj2sh3ykj27xkhwhcvwz32wllx6qtvzy4leyz8up6tgd98zkl" | cardano-address key child 1852H/1815H/0H | cardano-address key public --with-chain-code
    val acctExtendedVkey =
      "acct_xvk155crk6049ap0477qvjpf5mvxtw5f46uk6k54udc9mz5wcdyyhssexcsk5sgvy05m7mqh3ed3qgs6epyf7hvdfxf6hd54aqm3uwdsewqu6vsvy"

    val cardanoAddressNo = 5

    val cardanoAddressKey = CardanoAddressKey(
      "addr_xvk1q97wa0kvhw4xjchxm66f5ulrdkl50sw5lshxazgj28g34wnqj9fx28acsheajay7sk9ymgaghh8m7mlssly0aqwvedynyrs7apest4c3lhxja"
    )

    val cardanoAddress = CardanoAddress(
      "addr1qxhlq8kzxw5l0s0zrljg24pjsduf8vndtg5z2qhy9dm040xxrm8nx4l8qntadvlvdfldg8h3v7q7x6s0xcfg54g5c3qqmu2zvy"
    )
  }

  object CardanoAddressInfoFixtures {
    import ConnectionFixtures._
    import CredentialFixtures._

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
      received = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = plainTextCredentialMessage(jsonBasedCredential1, proof1).toByteString
    )

    lazy val credentialMessage2 = ReceivedMessage(
      id = "id2",
      received = Timestamp(LocalDateTime.of(2020, 6, 14, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId2.uuid.toString,
      message = plainTextCredentialMessage(jsonBasedCredential2, proof2).toByteString
    )

    val cardanoAddress1 = "cardanoAddress1"

    lazy val cardanoAddressInfoMessage1 = ReceivedMessage(
      id = "id3",
      received = Timestamp(LocalDateTime.of(2020, 6, 13, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = AtalaMessage()
        .withMirrorMessage(MirrorMessage().withRegisterAddressMessage(RegisterAddressMessage(cardanoAddress1)))
        .toByteString
    )

    lazy val initiateTrisaCardanoTransactionMessage = ReceivedMessage(
      id = "id1",
      received = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = AtalaMessage()
        .withMirrorMessage(
          MirrorMessage().withInitiateTrisaCardanoTransactionMessage(
            InitiateTrisaCardanoTransactionMessage(
              sourceCardanoAddress = "source",
              desinationCardanoAddress = "destination",
              lovelaceAmount = 10,
              trisaVaspHost = "vasp1",
              trisaVaspHostPortNumber = 8091
            )
          )
        )
        .toByteString
    )

    lazy val cardanoWalletName = "wallet name"
    lazy val cardanoExtendedPublicKey =
      "acct_xvk155crk6049ap0477qvjpf5mvxtw5f46uk6k54udc9mz5wcdyyhssexcsk5sgvy05m7mqh3ed3qgs6epyf7hvdfxf6hd54aqm3uwdsewqu6vsvy"

    lazy val cardanoRegisterWalletMessageMessage = ReceivedMessage(
      id = "id4",
      received = Timestamp(LocalDateTime.of(2020, 6, 13, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
      connectionId = connectionId1.uuid.toString,
      message = AtalaMessage()
        .withMirrorMessage(
          credential_models
            .MirrorMessage()
            .withRegisterWalletMessage(
              credential_models.RegisterWalletMessage(
                name = cardanoWalletName,
                extendedPublicKey = cardanoExtendedPublicKey
              )
            )
        )
        .toByteString
    )
  }

  object PayIdFixtures {
    import ConnectionFixtures._
    import CredentialFixtures._

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

    lazy val payIdNameRegistrationMessage1 =
      makeReceivedMessage(message = payIdNameRegistrationToAtalaMessage(payIdName1))

    def payIdNameRegistrationToAtalaMessage(payIdName: String): ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage().withPayIdNameRegistrationMessage(credential_models.PayIdNameRegistrationMessage(payIdName))
        )
        .toByteString

    def checkPayIdNameToAtalaMessage(payIdName: String): ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withCheckPayIdNameAvailabilityMessage(credential_models.CheckPayIdNameAvailabilityMessage(payIdName))
        )
        .toByteString

    val getPayIdNameToAtalaMessage: ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withGetPayIdNameMessage(credential_models.GetPayIdNameMessage())
        )
        .toByteString

    val getPayIdAddressesToAtalaMessage: ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withGetPayIdAddressesMessage(credential_models.GetPayIdAddressesMessage())
        )
        .toByteString

    val getRegisteredWalletsToAtalaMessage: ByteString =
      AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withGetRegisteredWalletsMessage(credential_models.GetRegisteredWalletsMessage())
        )
        .toByteString

    def makeReceivedMessage(
        id: String = "id1",
        received: Option[Timestamp] = Timestamp(LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)).some,
        connectionId: String = connectionId1.uuid.toString,
        message: ByteString
    ): ReceivedMessage = ReceivedMessage(id, connectionId, message, received)
  }

  object CardanoWalletFixtures {
    import ConnectionFixtures._
    val minAddressesCount = 10
    val extendedPublicKey =
      "acct_xvk155crk6049ap0477qvjpf5mvxtw5f46uk6k54udc9mz5wcdyyhssexcsk5sgvy05m7mqh3ed3qgs6epyf7hvdfxf6hd54aqm3uwdsewqu6vsvy"
    val network = "testnet"
    val cardanoConfig = CardanoConfig(config.CardanoNetwork.TestNet, minAddressesCount, syncIntervalInSeconds = 300)
    val cardanoAddressServiceStub: CardanoAddressService = new CardanoAddressServiceStub()

    val cardanoWallet1 = CardanoWallet(
      id = CardanoWallet.Id.random(),
      name = Some("name"),
      connectionToken = connection2.token,
      extendedPublicKey = extendedPublicKey,
      lastGeneratedNo = minAddressesCount - 1,
      lastUsedNo = None,
      registrationDate = CardanoWallet.RegistrationDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC))
    )

    val cardanoWallet2 = CardanoWallet(
      id = CardanoWallet.Id.random(),
      name = Some("name2"),
      connectionToken = connection2.token,
      extendedPublicKey = "key2",
      lastGeneratedNo = 9,
      lastUsedNo = None,
      registrationDate = CardanoWallet.RegistrationDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC))
    )

    var cardanoWalletAddress1 = CardanoWalletAddress(
      address = CardanoAddress(
        "addr_0"
      ),
      walletId = cardanoWallet1.id,
      sequenceNo = 0,
      usedAt = None
    )

    var cardanoWalletAddress2 = CardanoWalletAddress(
      address = CardanoAddress(
        "addr_1"
      ),
      walletId = cardanoWallet1.id,
      sequenceNo = 1,
      usedAt = None
    )

    lazy val otherWalletAddresses = cardanoAddressServiceStub
      .generateWalletAddresses(extendedPublicKey, fromSequenceNo = 2, minAddressesCount, network)
      .toOption
      .value
      .map { case (address, sequenceNo) => CardanoWalletAddress(address, cardanoWallet1.id, sequenceNo, None) }

    def insertAll[F[_]: Sync](database: Transactor[F]): F[Unit] = {
      for {
        _ <- insertManyFixtures(
          CardanoWalletDao.insert(cardanoWallet1),
          CardanoWalletDao.insert(cardanoWallet2)
        )(database)
        _ <- insertManyFixtures(
          CardanoWalletAddressDao.insertMany.updateMany(
            cardanoWalletAddress1 :: cardanoWalletAddress2 :: otherWalletAddresses
          )
        )(database)
      } yield ()
    }
  }

  object CardanoDBSyncFixtures {
    import CardanoWalletFixtures._

    val createBlockTable: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE IF NOT EXISTS block (
          id int8 NOT NULL PRIMARY KEY,
          time timestamp NOT NULL
        );
         """.update.run

    val createTxTable: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE IF NOT EXISTS tx (
          id int8 NOT NULL PRIMARY KEY,
          block_id int8 NOT NULL
        );
        ALTER TABLE tx DROP CONSTRAINT IF EXISTS tx_block_id_fkey;
        ALTER TABLE tx ADD CONSTRAINT tx_block_id_fkey FOREIGN KEY (block_id) REFERENCES block(id) ON UPDATE RESTRICT ON DELETE CASCADE;
         """.update.run

    val createTxOutTable: doobie.ConnectionIO[Int] =
      sql"""
        CREATE TABLE IF NOT EXISTS tx_out (
          id int8 NOT NULL PRIMARY KEY,
          tx_id int8 NOT NULL,
          address varchar NOT NULL
        );
        ALTER TABLE tx_out DROP CONSTRAINT IF EXISTS tx_out_tx_id_fkey;
        ALTER TABLE tx_out ADD CONSTRAINT tx_out_tx_id_fkey FOREIGN KEY (tx_id) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE CASCADE;
         """.update.run

    def createDbSyncSchema[F[_]: Sync](database: Transactor[F]): F[Unit] = {
      (for {
        _ <- createBlockTable
        _ <- createTxTable
        _ <- createTxOutTable
      } yield ()).transact(database)
    }

    val blocksCount = 53

    val insertBlockData: doobie.ConnectionIO[List[Int]] = 1.to(blocksCount).toList.traverse { id =>
      sql"INSERT INTO block(id, time) VALUES($id, '2019-07-24 20:20:16.000')".update.run
    }

    val insertTxData: doobie.ConnectionIO[Int] =
      sql"INSERT INTO tx(id, block_id) VALUES(1, 1)".update.run

    val insertTxOut: Update[(String, Int)] =
      Update[(String, Int)](
        """INSERT INTO tx_out(address, id, tx_id) values (?, ?, 1)""".stripMargin
      )

    def createAddressData(count: Int, cardanoAddressService: CardanoAddressService): List[(String, Int)] = {
      val fromSequenceNo = 0
      cardanoAddressService
        .generateWalletAddresses(extendedPublicKey, fromSequenceNo, count, network)
        .toOption
        .get
        .map { case (address, sequenceNo) => address.value -> sequenceNo }
    }

    def insert[F[_]: Sync](
        count: Int,
        cardanoAddressService: CardanoAddressService,
        database: Transactor[F]
    ): F[Unit] = {
      (for {
        _ <- insertBlockData
        _ <- insertTxData
        _ <- insertTxOut.updateMany(createAddressData(count, cardanoAddressService))
      } yield ()).transact(database)
    }

  }
}
