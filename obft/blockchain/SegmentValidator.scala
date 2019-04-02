package obft.blockchain

// format: off

import scala.annotation.tailrec
import obft.fakes._

// This validates that a segment of a Blockchain is correct (following the rules specified in the paper)
// This does not perform any kind of business logic of the data stored within Tx (or the Ledger for that matter)
class SegmentValidator(keys: List[PublicKey]) {

  // Checks that the content of a single (non genesis) block is valid
  def isValid[Tx](block: Block[Tx], hashPreviousBlock: Hash[AnyBlock[Tx]]): Boolean = {

    // > [...] h is the hash of the previous block [...]
    val body = block.body
    if (body.hash != hashPreviousBlock)
      return false

    // > [...] by server i such that i - 1 = (j - 1) mod n [...]
    // In the previous fragment:
    // * i is the `leaderIndex`
    // * j is `body.timeSlot`
    // * the `leader` method in `timeSlot` implements the formula i - 1 = (j - 1) mod n
    val leaderIndex = body.timeSlot.leader(keys.length)
    val leaderKey = keys(leaderIndex - 1) // `leaderIndex` is 1-indexed

    // > [...] contains proper signatures - one for time slot slj [...]
    if (!body.timeSlotSignature.isSignatureOf(body.timeSlot, leaderKey))
      return false

    // > [...] contains proper signatures - [...] and one for the entire block [...]
    if (!block.signature.isSignatureOf(body, leaderKey))
      return false

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
    return true
  }

  def isValid[Tx](
    chainSegment:      List[Block[Tx]],
    hashPreviousBlock: Hash[AnyBlock[Tx]]    // This is the hash of the block that `chainSegment` would follow if
                                             // chainSegment happened to be valid
  ): Boolean = {

    // This is implemented as a nested method because otherwise the @tailrec annotation only works on final or private
    // methods. And marking the external method as either private or final, would hinder unit testing.
    @tailrec
    def isSegmentValid(chainSegment: List[Block[Tx]], hashPreviousBlock: Hash[AnyBlock[Tx]]): Boolean =
      chainSegment match {
        case Nil =>
          true
        case h :: Nil =>
          // A segment is valid if ...
          isValid(h, hashPreviousBlock)
        case h :: h2 :: t =>
          // A segment is valid if ...
          isValid(h, Hash(h2)) &&                    // ... the head is valid
          isSegmentValid(h2 :: t, hashPreviousBlock) // ... and the tail (another chain segment) is valid
                                                     // NOTE: the tail, to be valid, has to accept the hash
                                                     //       of the head as valid
      }

    isSegmentValid(chainSegment, hashPreviousBlock)
  }

}
