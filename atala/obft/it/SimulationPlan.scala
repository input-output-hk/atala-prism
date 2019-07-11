package atala.obft.it

import atala.clock.TimeSlot
import atala.obft.it.SimulationItem.{SendTransaction, VerifyState}
import monix.eval.Task

import scala.util.Try

trait StateLaws[S, Tx] {
  def initialState: S

  def transition(state: S, transaction: Tx): Option[S]

  def applyTransaction(state: S, transaction: Tx): S = transition(state, transaction).getOrElse(state)
}

sealed trait SimulationItem[-S, +Tx]
sealed trait SimulationAction[+Tx] extends SimulationItem[Any, Tx]
sealed trait SimulationCheck[-S] extends SimulationItem[S, Nothing]

object SimulationItem {

  case class SendTransaction[+Tx](i: Int, transaction: Tx) extends SimulationAction[Tx]

  case class VerifyState[-S](check: S => Try[Unit]) extends SimulationCheck[S]

  object VerifyState {
    def catching[S](check: S => Any): VerifyState[S] = VerifyState { s: S =>
      Try(check(s))
    }
  }

}

case class SimulationSlot[-S, +Tx](actions: List[SimulationAction[Tx]], checks: List[SimulationCheck[S]])

case class SimulationPlan[S, Tx](
    n: Int,
    slots: Map[TimeSlot, SimulationSlot[S, Tx]],
    firstSlot: TimeSlot,
    lastSlot: TimeSlot
) {
  override def toString(): String = {
    val slotsString = slots.toSeq
      .sortBy(_._1)
      .map {
        case (ts, slot) =>
          val slotItemsString = (slot.actions ++ slot.checks).mkString("* ", "\n* ", "")
          s"$ts:\n$slotItemsString"
      }
      .mkString("\n")
    s"SimulationPlan(n = $n, \n$slotsString\n)"
  }

  def run(
      simulationName: String,
      verifyStateStability: Boolean = true
  )(implicit model: SimulationModel[Tx], stateLaws: StateLaws[S, Tx]): Task[Unit] = {
    model.runSimulation(this, simulationName, verifyStateStability)
  }
}

object SimulationPlan {

  class Builder[S, Tx](n: Int, t: Int, reversedItems: List[(TimeSlot, SimulationItem[S, Tx])]) {
    def withItem(slot: Long, item: SimulationItem[S, Tx]): Builder[S, Tx] = {
      new Builder(n, t, (TimeSlot.from(slot).get -> item) :: reversedItems)
    }

    def verify(slot: Long)(check: S => Any): Builder[S, Tx] = {
      withItem(slot, VerifyState.catching(check))
    }

    def send(slot: Long, node: Int, tx: Tx): Builder[S, Tx] = {
      withItem(slot, SendTransaction[Tx](node, tx))
    }

    def withItems[T](seq: Seq[(TimeSlot, SimulationItem[S, Tx])]): Builder[S, Tx] = {
      val newItems = seq.foldLeft(reversedItems)((its, el) => el :: its)
      new Builder(n, t, newItems)
    }

    def withItemsFrom[T](seq: Seq[T])(f: T => (TimeSlot, SimulationItem[S, Tx])): Builder[S, Tx] = {
      withItems(seq.map(f))
    }

    def build(): SimulationPlan[S, Tx] = {
      val slots = reversedItems.reverse.groupBy(_._1).mapValues { slotItems =>
        val actions = slotItems.collect { case (_, action: SimulationAction[Tx]) => action }
        val checks = slotItems.collect { case (_, check: SimulationCheck[S]) => check }
        SimulationSlot(actions, checks)
      }
      SimulationPlan(n, slots, TimeSlot.zero, slots.keys.max)
    }
  }

  def builder[S, Tx](n: Int, t: Int): Builder[S, Tx] = new Builder(n, t, List.empty)

}
