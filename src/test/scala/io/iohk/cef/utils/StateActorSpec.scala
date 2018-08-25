package io.iohk.cef.utils

import akka.actor.ActorSystem
import org.scalacheck.Arbitrary._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class StateActorSpec extends FlatSpec {

  behavior of "StateActor"

  implicit val actorSystem: ActorSystem = ActorSystem()

  it should "Get its initial value" in {
    forAll(arbitrary[String]) { s =>
      val state = new StateActor[String](s)

      state.get.futureValue shouldBe s
    }
  }

  it should "Set and Get an updated value" in {
    forAll(arbitrary[(String, String)]) { ss =>
      val state = new StateActor[String](ss._1)

      whenReady(state.set(ss._2)) { _ =>
        state.get.futureValue shouldBe ss._2
      }
    }
  }
}