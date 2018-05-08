package io.iohk.cef.network

import java.net.InetSocketAddress

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters


sealed trait ServerStatus
object ServerStatus {
  case object NotListening extends ServerStatus
  case class Listening(address: InetSocketAddress) extends ServerStatus
}

object NodeStatus {

  case class NodeState(
                    key: AsymmetricCipherKeyPair,
                    serverStatus: ServerStatus,
                    capabilities: Capabilities) extends  {

    val nodeId = key.getPublic.asInstanceOf[ECPublicKeyParameters].toNodeId
  }

  case class StateUpdated(state: NodeState)

  sealed trait NodeStatusMessage
  case class RequestState(replyTo: ActorRef[NodeState]) extends NodeStatusMessage
  case class UpdateState(capabilities: Capabilities) extends NodeStatusMessage
  case class Subscribe(ref: ActorRef[StateUpdated]) extends NodeStatusMessage

  def nodeState(state: NodeState, subscribers: Seq[ActorRef[StateUpdated]]): Behavior[NodeStatusMessage] = Behaviors.setup { context =>

    Behaviors.receiveMessage { message =>
      message match {
        case request: RequestState =>
          request.replyTo ! state
          Behavior.same
        case update: UpdateState =>
          val newState = state.copy(capabilities = update.capabilities)
          subscribers.foreach(_ ! StateUpdated(newState))
          nodeState(newState, subscribers)
        case subscribe: Subscribe =>
          subscribe.ref ! StateUpdated(state)
          nodeState(state, subscribers :+ subscribe.ref)
      }
    }
  }
}
