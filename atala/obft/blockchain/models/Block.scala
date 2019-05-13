package atala.obft.blockchain.models

import atala.clock._
import io.iohk.multicrypto._
import io.iohk.decco.Codec
import io.iohk.decco.auto._

// format: off

/** Encapsulates the height of a block/blockchain
  *
  * @param int height represented
  * @note the internal representation may need to change to Long/BigInt
  */
class Height private (val int: Int) extends AnyVal {
  def toInt: Int = int
  def above: Height = new Height(int + 1)
  def below: Height = new Height(int - 1)
}

object Height {
  val Zero = new Height(0)
  def from(int: Int): Option[Height] = {
    if (int < 0) None
    else Some(new Height(int))
  }

  implicit val heightCodec: Codec[Height] =
    Codec[Int].map(i => Height.from(i).getOrElse(throw new RuntimeException("heightCodec: Corrupted height")), _.toInt)

}


// There are two types of blocks, but only one should be publicly used. For
// that reason, I'll reserve the name `Block` for the subtype that is commonly used;
// `AnyBlock` for the common parent and `GenesisBlock` for the other subtype.
//
// A good example on why *only* `Block` should be publically used is that, in the
// `AddBlockchainSegment` message type, only regular blocks should be allowed. It makes
// no sense to be sending the `GenesisBlock` around
sealed trait AnyBlock[Tx] {
  def height: Height
}



// Quoting the paper:
//   > [...] beginning with a special "genesis" block B0 which contains the servers'
//   > public-keys (vk1,...,vkn).
final case class GenesisBlock[Tx](keys: List[SigningPublicKey]) extends AnyBlock[Tx] {
  override val height: Height = Height.Zero
}




// Quoting the paper:
//   > Each subsequent block Bi, i > 0, is a quintuple of the form (h, d, sl, σsl, σblock),
//   > where [...] σblock is a signature (of the entire block).
//
// Given that it's impossible to generate the signature of the signature itself, the previous
// fragment should read:
//   σblock is a signature (of the tuple with the four previous fields: (h, d, sl, σsl))
//
// For that purpose, I've grouped up the first four fields in it's own case class (`BlockBody`)
// and `Block` contains that and the signature of that.
final case class Block[Tx](
    override val height: Height,
    body: BlockBody[Tx],
    // AKA σblock in the paper.
    //   > σblock is a signature (of the entire block).
    signature: Signature
) extends AnyBlock[Tx]


// This contains all the data of a Block, except it's signature
final case class BlockBody[Tx](
    // AKA h in the paper.
    //   > h is the hash of the previous block
    previousHash: Hash,

    // AKA d in the paper.
    //   > d is a set of transactions
    delta: List[Tx],

    // AKA sl in the paper.
    //   > sl is a (slot) time-stamp
    timeSlot: TimeSlot,

    // AKA σsl in the paper.
    //   > σsl is a signature of the slot number
    timeSlotSignature: Signature
)
