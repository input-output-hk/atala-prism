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

  it should "Fail to get a nonexistant value" in {
    val state = new StateActor[String]()
    state.get.failed.futureValue shouldBe an[IllegalStateException]
  }

  it should "Enable get to succeed if set is invoked before a timeout" in {
    val state = new StateActor[String]()
    val getF = state.get
    state.set("a value")

    whenReady(getF) { value =>
      value shouldBe "a value"
    }
  }

  it should "Set and Get a value" in {
    forAll(arbitrary[String]) { s =>
      val state = new StateActor[String]()
      whenReady(state.set(s)) { _ =>
        state.get.futureValue shouldBe s
      }
    }
  }
}