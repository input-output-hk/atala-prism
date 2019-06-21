package atala.obft

// format: off

import atala.clock.TimeSlot
import atala.obft.blockchain.models.ChainSegment

// OuroborosBFT is implemented around the concept of an 'actor' implemented with a
// monix stream. ObftActorMessage is the base type of the messages processed by the
// actor.
//
// ObftActorMessage = ObftExternalActorMessage | ObftInternalActorMessage
//

sealed trait ObftActorMessage[Tx]

sealed trait ObftExternalActorMessage[Tx] extends ObftActorMessage[Tx]
sealed trait ObftInternalActorMessage[Tx] extends ObftActorMessage[Tx]




// ObftExternalActorMessage represent the messages generated from the outside of the
// actor itself. In practical terms, it represents the messages or events described
// in the OuroborosBFT paper (specified in its Figure 1)
//
// ObftExternalActorMessage = Tick | NetworkMessage
//

final case class Tick[Tx](timeSlot: TimeSlot) extends ObftExternalActorMessage[Tx]

// This represents the messages used to communicate with the other OBFT nodes (from the
// point of view of the OuroborosBFT algorithm)
sealed trait NetworkMessage[Tx] extends ObftExternalActorMessage[Tx]
object NetworkMessage {

  final case class AddTransaction[Tx](tx: Tx) extends NetworkMessage[Tx]
  final case class AddBlockchainSegment[Tx](chainSegment: ChainSegment[Tx]) extends NetworkMessage[Tx]
}




// ObftInternalActorMessage represent the messages generated from within the OuroborosBFT
// instance and will never be serialized through the wire (or stored)
//
// ObftInternalActorMessage = RequestStateUpdate
//

final case class RequestStateUpdate[Tx](action: () => Unit) extends ObftInternalActorMessage[Tx]
