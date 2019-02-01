package io.iohk.cef.agreements

import io.iohk.network.{ConversationalNetwork, NodeId}
import io.iohk.network.transport.tcp.NetUtils
import io.iohk.codecs.nio._
import io.iohk.codecs.nio.auto._
import io.iohk.network.transport.tcp.NetUtils.NetworkFixture
import io.iohk.crypto._
import scala.reflect.runtime.universe.TypeTag

case class AgreementFixture[T](nodeId: NodeId, keyPair: SigningKeyPair, agreementsService: AgreementsService[T])

object AgreementFixture {
  def forTwoArbitraryAgreementPeers[T: NioCodec: TypeTag](
      testCode: (AgreementFixture[T], AgreementFixture[T]) => Any
  ): Unit = {

    NetUtils.forTwoArbitraryNetworkPeers { (aliceFix, bobFix) =>
      val aliceNet = new ConversationalNetwork[AgreementMessage[T]](aliceFix.networkDiscovery, aliceFix.transports)
      val aliceAgreementService = new AgreementsServiceImpl[T](aliceNet)

      val bobNet = new ConversationalNetwork[AgreementMessage[T]](bobFix.networkDiscovery, bobFix.transports)
      val bobAgreementService = new AgreementsServiceImpl[T](bobNet)

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
      val aliceAgreementService = new AgreementsServiceImpl[T](aliceNet)

      val bobFix: NetworkFixture = peers(1)
      val bobNet = new ConversationalNetwork[AgreementMessage[T]](bobFix.networkDiscovery, bobFix.transports)
      val bobAgreementService = new AgreementsServiceImpl[T](bobNet)

      val charlieFix: NetworkFixture = peers(2)
      val charlieNet =
        new ConversationalNetwork[AgreementMessage[T]](charlieFix.networkDiscovery, charlieFix.transports)
      val charlieAgreementService = new AgreementsServiceImpl[T](charlieNet)

      testCode(
        AgreementFixture(aliceFix.nodeId, generateSigningKeyPair(), aliceAgreementService),
        AgreementFixture(bobFix.nodeId, generateSigningKeyPair(), bobAgreementService),
        AgreementFixture(charlieFix.nodeId, generateSigningKeyPair(), charlieAgreementService)
      )
    }

  }
}
