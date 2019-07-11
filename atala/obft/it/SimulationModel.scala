package atala.obft.it

import atala.clock.TimeSlot
import atala.logging.Loggable
import atala.obft.it.SimulationItem.{SendTransaction, VerifyState}
import atala.obft.{NetworkMessage, OuroborosBFT, Tick}
import io.iohk.decco.Codec
import io.iohk.multicrypto.generateSigningKeyPair
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.{ConcurrentSubject, PublishSubject}
import monix.reactive.{Observable, Observer}
import org.scalatest.MustMatchers._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

case class ObftNetwork[Tx](
    nodeInputs: List[Observable[NetworkMessage[Tx]] with Observer[NetworkMessage[Tx]]],
    nodeOutputs: List[Observer[NetworkMessage[Tx]]]
)

object ObftNetwork {
  def monix[Tx](n: Int): ObftNetwork[Tx] = {
    val nodeInputs = List.fill(n)(ConcurrentSubject.publishToOne[NetworkMessage[Tx]])
    val nodeOutputs = List.fill(n)(PublishSubject[NetworkMessage[Tx]]())
    for (out <- nodeOutputs; in <- nodeInputs) {
      out.subscribe(in)
    }
    ObftNetwork[Tx](nodeInputs, nodeOutputs)
  }
}

trait SimulationModel[Tx] {
  def runSimulation[S](plan: SimulationPlan[S, Tx], simulationName: String, verifyClusterConsensus: Boolean)(
      implicit stateLaws: StateLaws[S, Tx]
  ): Task[Unit]
}

object BasicSimulationModel {

  class Node[S, Tx: Codec](obft: OuroborosBFT[Tx], val input: Observer[NetworkMessage[Tx]], sl: StateLaws[S, Tx]) {
    var state = sl.initialState
    var lastTimeSlot = TimeSlot.zero

    obft.view.foreach { transactionSnapshot =>
      state = sl.transition(state, transactionSnapshot.transaction).getOrElse(state)
      lastTimeSlot = transactionSnapshot.timestamp
    }
  }

  case class State[S, Tx](nodes: List[Node[S, Tx]], tickStream: Observer[Tick[Tx]])

}

class BasicSimulationModel[Tx: Codec: Loggable](slotDuration: FiniteDuration, generateNetwork: Int => ObftNetwork[Tx])
    extends SimulationModel[Tx] {

  val logger = LoggerFactory.getLogger(this.getClass)

  import BasicSimulationModel._

  protected def initialize[S](n: Int, simulationName: String, stateLaws: StateLaws[S, Tx]): State[S, Tx] = {
    val keys = List.fill(n)(generateSigningKeyPair())
    val publicKeys = keys.map(_.public)
    val tickStream = ConcurrentSubject.publish[Tick[Tx]]
    val ObftNetwork(nodeInputs, nodeOutputs) = generateNetwork(n)

    val nodes = keys.zip(nodeInputs).zip(nodeOutputs).zipWithIndex.map {
      case (((key, input), output), index) =>
        val obft = OuroborosBFT[Tx](
          i = index + 1,
          initialTimeSlot = TimeSlot.zero,
          keyPair = key,
          genesisKeys = publicKeys,
          inputStreamClockSignals = tickStream,
          inputStreamMessages = input,
          outputStreamDiffuseToRestOfCluster = output,
          s"database_${simulationName}_${index + 1}",
          lastProcessedTimeSlot = TimeSlot.zero
        )
        new Node[S, Tx](obft, input, stateLaws)
    }

    State(nodes, tickStream)
  }

  protected def actionsPhase[S](state: State[S, Tx], actions: Seq[SimulationAction[Tx]]): Task[Unit] = Task {
    for (action <- actions) action match {
      case SendTransaction(i, tx) =>
        logger.info(s"Sending transaction to $i: $tx")
        state.nodes(i - 1).input.onNext(NetworkMessage.AddTransaction(tx))
    }
  }

  protected def checksPhase[S](
      state: State[S, Tx],
      checks: Seq[SimulationCheck[S]],
      verifyClusterConsensus: Boolean
  ): Task[Unit] = Task {
    val nodeStates = state.nodes.map(_.state)

    if (verifyClusterConsensus) {
      nodeStates.tail.foreach { nodeState =>
        nodeState mustBe nodeStates.head
      }
    }

    for (check <- checks) check match {
      case VerifyState(checkPredicate) =>
        nodeStates.foreach(state => checkPredicate(state).get)
    }
  }

  override def runSimulation[S](plan: SimulationPlan[S, Tx], simulationName: String, verifyClusterConsensus: Boolean)(
      implicit stateLaws: StateLaws[S, Tx]
  ): Task[Unit] = {
    val n = plan.n

    val state = initialize(n, simulationName, stateLaws)

    val firstSlot = TimeSlot.zero.next
    val lastSlot = plan.lastSlot

    val slots = Observable
      .fromStateAction((slot: TimeSlot) => (slot, slot.next))(firstSlot)
      .takeWhile(_.compare(lastSlot) <= 0)

    slots.mapTask { slot =>
      logger.info(s"Simulating slot $slot")
      val simulationSlotOpt: Option[SimulationSlot[S, Tx]] = plan.slots.get(slot)

      for {
        _ <- simulationSlotOpt.fold(Task.unit)(slot => actionsPhase(state, slot.actions))
        _ <- Task.fromFuture { state.tickStream.onNext(Tick(slot)) }
        _ <- Task.sleep(slotDuration)
        _ <- simulationSlotOpt.fold(Task.unit)(slot => checksPhase(state, slot.checks, verifyClusterConsensus))
      } yield ()
    }.lastL
  }
}
