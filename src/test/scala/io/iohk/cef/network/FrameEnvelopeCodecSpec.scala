package io.iohk.cef.network

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.network.transport.{Frame, FrameHeader}
import io.iohk.cef.test.DummyTransaction
import io.iohk.cef.core.DestinationDescriptor
import io.iohk.cef.core.Everyone
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, BlockHeader}

class FrameEnvelopeCodecSpec extends FlatSpec {
  behavior of "FrameEnvelopeEncoder"

  it should "do envelope encoding and decoding properly" in {
    val testTx = DummyTransaction(10)

    test(testTx)
    test(Everyone: DestinationDescriptor)
    test("1")
    val envelope = Envelope(testTx, "1", Everyone)
    test(envelope)
    test(Frame(FrameHeader(NetUtils.aRandomNodeId(), NetUtils.aRandomNodeId()), envelope))
    val h = BlockHeader()
    test(h)
    test(List(testTx))
    val b: Block[String, DummyTransaction] = Block(h, Seq(testTx))
    test(b)
    test(Envelope(b, "1", Everyone))
    test(Frame(FrameHeader(NetUtils.aRandomNodeId(), NetUtils.aRandomNodeId()), Envelope(b, "1", Everyone)))

  }

  def test[T: NioEncoder: NioDecoder](r: T) = {

    val enc = implicitly[NioEncoder[T]]
    val dec = implicitly[NioDecoder[T]]

    if (enc == null)
      fail("Impossible!!!")

    val encoded = enc.encode(r)
    if (encoded == null)
      fail("For some reason, the authomatic encoder is 'null'")

    val decoded = dec.decode(encoded)
    if (decoded == null)
      fail("For some reason, the authomatic decoder is 'null'")

    dec.decode(enc.encode(r)) shouldBe Some(r)
  }

  it should "do frame encoding and decoding properly" in {
    val testTx = DummyTransaction(10)

    val envelope: Envelope[DummyTransaction] = Envelope(testTx, "1", Everyone)

    val enc = implicitly[NioEncoder[Frame[Envelope[DummyTransaction]]]]
    val dec = implicitly[NioDecoder[Frame[Envelope[DummyTransaction]]]]

    val frame = Frame(FrameHeader(NetUtils.aRandomNodeId(), NetUtils.aRandomNodeId()), envelope)

    dec.decode(enc.encode(frame)) shouldBe Some(frame)
  }

}
