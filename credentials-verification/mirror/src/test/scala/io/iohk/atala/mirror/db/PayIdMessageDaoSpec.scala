package io.iohk.atala.mirror.db

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.mirror.models.{ConnectorMessageId, PayIdMessage}
import io.circe.syntax._
import io.iohk.atala.prism.mirror.payid.implicits._

// sbt "project mirror" "testOnly *db.PayIdMessageDaoSpec"
class PayIdMessageDaoSpec extends PostgresRepositorySpec with MirrorFixtures {
  import PayIdFixtures._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "PayIdMessageDao" should {
    "insert single pay id message into the db" in {
      // when
      val resultCount = PayIdMessageDao
        .insert(
          PayIdMessage(
            ConnectorMessageId(paymentInformationMessage1.id),
            PayIdMessage.RawPaymentInformation(paymentInformation1.asJson.toString())
          )
        )
        .transact(database)
        .unsafeRunSync()

      // then
      resultCount mustBe 1
    }

    "return pay id message by id" in {
      val payIdMessage = PayIdMessage(
        ConnectorMessageId(paymentInformationMessage1.id),
        PayIdMessage.RawPaymentInformation(paymentInformation1.asJson.toString())
      )

      // given
      PayIdMessageDao
        .insert(payIdMessage)
        .transact(database)
        .unsafeRunSync()

      // when
      val payIdMessageOption =
        PayIdMessageDao.findBy(ConnectorMessageId(paymentInformationMessage1.id)).transact(database).unsafeRunSync()

      // then
      payIdMessageOption mustBe Some(payIdMessage)
    }

    "return none if a pay id message doesn't exist" in {
      // when
      val payIdMessage =
        CardanoAddressInfoDao.findBy(CardanoAddress("non existing")).transact(database).unsafeRunSync()

      // then
      payIdMessage.size mustBe 0
    }
  }
}
