package io.iohk.cef.agreements

import io.iohk.cef.network.{ConversationalNetwork, NodeId}
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.network.transport.tcp.NetUtils.NetworkFixture
import io.iohk.cef.crypto._
import scala.reflect.runtime.universe.TypeTag

case class AgreementFixture[T](nodeId: NodeId, keyPair: SigningKeyPair, agreementsService: AgreementsService[T])

object AgreementFixture {
  def forTwoArbitraryAgreementPeers[T: NioCodec: TypeTag](
      testCode: (AgreementFixture[T], AgreementFixture[T]) => Any
  ): Unit = {

    NetUtils.forTwoArbitraryNetworkPeers { (aliceFix, bobFix) =>
      val aliceNet = new ConversationalNetwork[AgreementMessage[T]](aliceFix.networkDiscovery, aliceFix.transports)
      val aliceAgreementService = new AgreementsService[T](aliceNet)

      val bobNet = new ConversationalNetwork[AgreementMessage[T]](bobFix.networkDiscovery, bobFix.transports)
      val bobAgreementService = new AgreementsService[T](bobNet)

      testCode(
        AgreementFixture(aliceFix.nodeId, generateSigningKeyPair(), aliceAgreementService),
        AgreementFixture(bobFix.nodeId, generateSigningKeyPair(), bobAgreementService)
      )
    }
  }

  def forThreeArbitraryAgreementPeers[T: NioCodec: TypeTag](
      testCode: (AgreementFixture[T], AgreementFixture[T], AgreementFixture[T]) => Any
  ): Unit = {
    NetUtils.forNArbitraryNetworkPeers(3) { peers =>
      val aliceFix: NetworkFixture = peers(0)
      val aliceNet = new ConversationalNetwork[AgreementMessage[T]](aliceFix.networkDiscovery, aliceFix.transports)
      val aliceAgreementService = new AgreementsService[T](aliceNet)

      val bobFix: NetworkFixture = peers(1)
      val bobNet = new ConversationalNetwork[AgreementMessage[T]](bobFix.networkDiscovery, bobFix.transports)
      val bobAgreementService = new AgreementsService[T](bobNet)

      val charlieFix: NetworkFixture = peers(2)
      val charlieNet =
        new ConversationalNetwork[AgreementMessage[T]](charlieFix.networkDiscovery, charlieFix.transports)
      val charlieAgreementService = new AgreementsService[T](charlieNet)

      testCode(
        AgreementFixture(aliceFix.nodeId, generateSigningKeyPair(), aliceAgreementService),
        AgreementFixture(bobFix.nodeId, generateSigningKeyPair(), bobAgreementService),
        AgreementFixture(charlieFix.nodeId, generateSigningKeyPair(), charlieAgreementService)
      )
    }

  }
}
