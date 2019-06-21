package atala.obft
package it

import atala.clock.TimeSlot
import atala.logging.Loggable
import atala.obft.it.SimulationItem.{SendTransaction, VerifyState}
import io.iohk.decco.Codec
import io.iohk.decco.auto._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Prop, Test}
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class MapStateLaws[K, V]() extends StateLaws[Map[K, V], (K, V)] {
  override def initialState: Map[K, V] = Map.empty

  override def transition(state: Map[K, V], transaction: (K, V)): Option[Map[K, V]] = {
    if (state.contains(transaction._1)) {
      None
    } else {
      Some(state + transaction)
    }
  }
}

case class ListAccumulation[T]() extends StateLaws[List[T], T] {
  override def initialState: List[T] = List.empty

  override def transition(state: List[T], transaction: T): Option[List[T]] = Some(transaction :: state)
}

class ObftSimulationTest extends WordSpec {
  val logger = LoggerFactory.getLogger(this.getClass)

  val RANDOM_SEED = 47414
  val MAX_COLLECTION_SIZE = 10
  val MIN_SUCCESFUL_TESTS = 10

  implicit val patienceConfig = PatienceConfig(timeout = 1.minute)
  implicit val scalaCheckParameters = Test.Parameters.default
    .withInitialSeed(RANDOM_SEED)
    .withMaxSize(MAX_COLLECTION_SIZE)
    .withMinSuccessfulTests(MIN_SUCCESFUL_TESTS)

  implicit val intStringMapStateLaws = MapStateLaws[Int, String]()
  implicit val intListStateLaws = ListAccumulation[Int]()

  implicit def defaultModel[Tx: Codec: Loggable]: SimulationModel[Tx] = new BasicSimulationModel[Tx](
    slotDuration = 500.millis,
    generateNetwork = ObftNetwork.monix[Tx]
  )

  def propResultFromTask(task: Task[Any]): Prop = {
    task.runAsync.transform {
      case Success(_) => Success(Prop.passed)
      case Failure(ex) => Success(Prop.exception(ex))
    }.futureValue
  }

  def checkResult(result: Test.Result): Unit = {
    result.status match {
      case Test.Passed => ()
      case Test.Exhausted => ()
      case Test.Proved(_) => ()
      case Test.Failed(_, _) =>
        fail("Property does not hold")
      case Test.PropException(_, ex, _) =>
        fail(ex)
    }
  }

  // test disabled until A-881 is done
  "should finalize transactions properly" ignore {
    // store random values under indexes for some prefix of alphabet (e.g. A, B, C, D, E)
    // and then verify the state contains all after time needed for finalizing
    val alphabetSize = 'Z' - 'A' + 1
    val simulationPlanGen = for {
      n <- Gen.chooseNum(2, 7)
      t <- Gen.chooseNum(0, (n - 1) / 3)
      slotsNumber <- Gen.chooseNum(1, alphabetSize)
    } yield {
      val transactions = (0 until slotsNumber).map { i =>
        (i + 1, ('A' + i).toString)
      }

      val accumulatedState =
        transactions.scanLeft(intStringMapStateLaws.initialState)(intStringMapStateLaws.applyTransaction).toList

      SimulationPlan
        .builder[Map[Int, String], (Int, String)](n, t)
        .withItems(transactions.zip(accumulatedState.sliding(2).toIterable).flatMap {
          case (tx, List(beforeState, afterState)) =>
            // time slot of insertion is equal to the index of transaction
            val ts = tx._1

            val insertion = (1 to n).map(node => TimeSlot.from(ts).get -> SendTransaction(node, tx))

            val checks = Seq(
              TimeSlot.from(ts + 3 * t + 1).get -> VerifyState
                .catching[Map[Int, String]](state => state mustBe beforeState),
              TimeSlot.from(ts + 3 * t + 2).get -> VerifyState
                .catching[Map[Int, String]](state => state mustBe afterState)
            )

            insertion ++ checks ++ Seq.empty
        })
        .build()
    }
    implicit val simulationPlan = Arbitrary(simulationPlanGen)

    val ids = Stream.from(1, 1).toIterator

    val prop = forAll { plan: SimulationPlan[Map[Int, String], (Int, String)] =>
      val id = ids.next()
      logger.info(s"Running simulation $id with plan:\n${plan.toString()}")

      propResultFromTask(plan.run(s"alphabet-$id"))
    }

    checkResult(Test.check(scalaCheckParameters, prop))
  }

  "should maintain consistent state for random transactions" in {
    val MAX_SLOTS = 30

    val simulationPlanGen = for {
      n <- Gen.chooseNum(2, 10)
      t <- Gen.chooseNum(0, (n - 1) / 3)
      slotsNumber <- Gen.chooseNum(1, MAX_SLOTS)
      slotValues <- Gen.nonEmptyListOf {
        for {
          slot <- Gen.chooseNum(1, slotsNumber)
          node <- Gen.chooseNum(1, n)
          value <- Gen.posNum[Int]
        } yield (slot, node, value)
      }
    } yield {
      SimulationPlan
        .builder[List[Int], Int](n, t)
        .withItemsFrom(slotValues) {
          case (slot, node, value) =>
            TimeSlot.from(slot).get -> SendTransaction(node, value)
        }
        .build()
    }
    implicit val simulationPlan = Arbitrary(simulationPlanGen)

    val ids = Stream.from(1, 1).toIterator

    val prop = forAll { plan: SimulationPlan[List[Int], Int] =>
      val id = ids.next()
      logger.info(s"Running simulation $id with plan:\n${plan.toString()}")

      propResultFromTask(plan.run(s"random-$id"))
    }
    checkResult(Test.check(scalaCheckParameters, prop))
  }

}
