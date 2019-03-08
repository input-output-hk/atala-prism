package obft.blockchain

import obft._

// format: off


// There are two types of blocks, but only one shuld be publically used. For
// that reason, I'll reserve the name `Block` for the subtype that is commonly used;
// `AnyBlock` for the common parent and `GenesisBlock` for the other subtype.
//
// A good example on why *only* `Block` should be publically used is that, in the
// `AddBlockchainSegment` message type, only regular blocks should be allowed. It makes
// no sense to be sending the `GenesisBlock` around
sealed trait AnyBlock[Tx]




// Quoting the paper:
//   > [...] beginning with a special "genesis" block B0 which contains the servers'
//   > public-keys (vk1,...,vkn).
final case class GenesisBlock[Tx](keys: List[PublicKey]) extends AnyBlock[Tx]




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
    body: BlockBody[Tx],
    signature: Signature[BlockBody[Tx]]              // AKA σblock in the paper.
                                                     //   > σblock is a signature (of the entire block).
) extends AnyBlock[Tx]




// This contains all the data of a Block, except it's signature
final case class BlockBody[Tx](

    hash: Hash[AnyBlock[Tx]],                        // AKA h in the paper.
                                                     //   > h is the hash of the previous block

    delta: List[Tx],                                 // AKA d in the paper.
                                                     //   > d is a set of transactions

    timeSlot: TimeSlot,                              // AKA sl in the paper.
                                                     //   > sl is a (slot) time-stamp

    timeSlotSignature: Signature[TimeSlot]           // AKA σsl in the paper.
                                                     //   > σsl is a signature of the slot number
)
