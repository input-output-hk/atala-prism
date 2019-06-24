package atala.obft.blockchain

// format: off

import atala.clock.TimeSlot

import scala.annotation.tailrec
import atala.obft.blockchain.models._
import io.iohk.decco._
import io.iohk.decco.auto._
import io.iohk.multicrypto._

// This validates that a segment of a Blockchain is correct (following the rules specified in the paper)
// This does not perform any kind of business logic of the data stored within Tx (or the Ledger for that matter)
trait SegmentValidator {

  def isValid[Tx : Codec](block: Block[Tx], previousBlock: AnyBlock[Tx], now: TimeSlot): Boolean

  def isValid[Tx : Codec](chainSegment: ChainSegment[Tx], previousBlock: AnyBlock[Tx], now: TimeSlot): Boolean
}

object SegmentValidator {
  def apply(keys: List[SigningPublicKey]): SegmentValidator = new SegmentValidatorImpl(keys)

  class SegmentValidatorImpl(val keys: List[SigningPublicKey]) extends SegmentValidator {

    // Checks that the content of a single (non genesis) block is valid
    override def isValid[Tx : Codec](block: Block[Tx], previousBlock: AnyBlock[Tx], now: TimeSlot): Boolean = {

      // > [...] h is the hash of the previous block [...]
      val currentBody = block.body
      def blockHashMatchesWithPreviousHash: Boolean = currentBody.previousHash == hash(previousBlock)

      // the time slots should be strictly increasing
      def increasesPreviousTimeSlot: Boolean = previousBlock match {
        case GenesisBlock(_) => true
        case Block(previousBody, _) => previousBody.timeSlot < currentBody.timeSlot
      }

      // the block is not coming from the future
      //
      // NOTE:   we allow blocks from the time slot immidiately after now, because even with a small
      //         clock delay it is possible that other nodes generate blocks (after their tick) and
      //         deliver them before the tick in this node
      //
      // NOTE 2: once we have implemented the logical clock, we could review this and decide if
      //         we still need it
      def blockIsNotFromTheFuture: Boolean = currentBody.timeSlot <= now.next


      // > [...] by server i such that i - 1 = (j - 1) mod n [...]
      // In the previous fragment:
      // * i is the `leaderIndex`
      // * j is `body.timeSlot`
      // * the `leader` method in `timeSlot` implements the formula i - 1 = (j - 1) mod n
      val leaderIndex = currentBody.timeSlot.leader(keys.length)
      val leaderKey = keys(leaderIndex - 1) // `leaderIndex` is 1-indexed


      // > [...] contains proper signatures - one for time slot slj [...]
      def timeSlotIsSignedByCorrectKey: Boolean = isValidSignature(currentBody.timeSlot, currentBody.timeSlotSignature, leaderKey)


      // > [...] contains proper signatures - [...] and one for the entire block [...]
      def blockIsSignedByCorrectKey: Boolean = isValidSignature(currentBody, block.signature, leaderKey)


      // NOTE: The paper also contains this fragment:
      // > [...] d is a valid sequence of transactions w.r.t. the ledger dfined by the transactions
      // > found in the previous blocks.
      // where `d` is the list of transactions wrapped in the block.
      //
      // I'm skipping this step for several reasons:
      // * Elsewere, the paper talks about the validity of a single transaction, not a whole list of them
      // * The paper talks about the fact that:
      //     > [...] some transactions in the mempool may be invalid for the state they are applied to; this
      //     > does not affect the validity of tx [...]
      //   where tx is the transaction we are trying to validate
      // * The implementation I've made of the blockchain doesn't store (or care about) the state, it's just
      //   a list of transactions. Without state, the validity of a single transaction is impossible to
      //   validate
      // * It's totally ok to have transactions that are invalid given some business logic, given that:
      //     > A transaction is invalid for state q if Rtx(q) = ⊥ and in this case we insist that q -tx-> q.
      //   I read the previous fragment as: if a transaction for a given state is invalid, when executing
      //   that transaction, the state should not change. That is: if a transaction is invalid, it means
      //   it behaves like an 'identity' transaction, but doesn't become a non-executable transaction.


      // If all the validations pass, the block itself is valid
      blockHashMatchesWithPreviousHash &&
        increasesPreviousTimeSlot &&
        blockIsNotFromTheFuture &&
        timeSlotIsSignedByCorrectKey &&
        blockIsSignedByCorrectKey
    }

    /**
      * This is the hash of the block that `chainSegment` would follow if chainSegment happened to be valid.
      */
    override def isValid[Tx : Codec](chainSegment: ChainSegment[Tx], previousBlock: AnyBlock[Tx], now: TimeSlot): Boolean = {

      // This is implemented as a nested method because otherwise the @tailrec annotation only works on final or private
      // methods. And marking the external method as either private or final, would hinder unit testing.
      @tailrec
      def isSegmentValid(acc: Boolean, chainSegment: ChainSegment[Tx]): Boolean = {
        chainSegment.blocks match {
          case Nil => acc
          case h :: Nil => acc && isValid(h, previousBlock, now)
          case h :: h2 :: t =>
            // A segment is valid if ...
            // ... the head is valid
            // ... and the tail (another chain segment) is valid
            // NOTE: the tail, to be valid, has to accept the hash
            //       of the head as valid
            isSegmentValid(acc && isValid(h, h2, now), ChainSegment[Tx](h2 :: t))
        }
      }

      isSegmentValid(true, chainSegment)
    }
  }

}
