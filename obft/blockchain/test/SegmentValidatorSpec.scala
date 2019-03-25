package obft.blockchain
package test

// format: off

import obft.clock._
import obft.fakes._

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

class SegmentValidatorSpec extends WordSpec with MustMatchers {

  "SegmentValidator.isValid(Block)" should {

    "accept a valid Block" in {
      segmentValidator.isValid(validBlock, validBlock.body.hash) mustBe true
    }

    "reject a Block with an incorrect previous hash" in {
      segmentValidator.isValid(validBlock, Mock.Hash("bad")) mustBe false
    }

    "reject a Block with the timeSlot signed by the wrong key" in {
      val block =
        validBlock.copy(body = validBlock.body.copy(timeSlotSignature = Signature.sign(TimeSlot(1230), wrongKeys.PrivateKey)))

      segmentValidator.isValid(block, block.body.hash) mustBe false
    }

    "reject a Block with the timeSlotSignature signing the incorrect timeSlot" in {
      val block =
        validBlock.copy(body = validBlock.body.copy(timeSlotSignature = Signature.sign(TimeSlot(1122), validSigningKey)))

      segmentValidator.isValid(block, block.body.hash) mustBe false
    }

    "reject a Block whose body is signed with the wrong key" in {
      val block =
        validBlock.copy(signature = Signature.sign(validBlock.body, wrongKeys.PrivateKey))

      segmentValidator.isValid(block, block.body.hash) mustBe false
    }

    "reject a Block whose signature is signing an incorrect body" in {
      val block =
        validBlock.copy(signature = Signature.sign(validBlock.body.copy(delta = Nil), validSigningKey))

      segmentValidator.isValid(block, block.body.hash) mustBe false
    }
  }

  "SegmentValidator.isValid(Segment)" should {

    "accept a valid segment" in {
      val validSegment = segment(10)
      segmentValidator.isValid(validSegment, validHash) mustBe true
    }

    "accept an empty segment" in {
      segmentValidator.isValid(Nil, validHash) mustBe true
    }

    "accept a segment with a single block, that is valid" in {
      segmentValidator.isValid(validBlock :: Nil, validHash) mustBe true
    }

    "reject a segment that contains an invalid block" in {
      val invalidSegment =
        segment(10)
          .zipWithIndex
          .map{
            case (b, 5) =>
              b.copy(signature = Signature.sign(b.body, wrongKeys.PrivateKey))
            case (b, _) => b
          }
      segmentValidator.isValid(invalidSegment, validHash) mustBe false
    }

    "reject a segment that doesn't end with the requested hash" in {
      val invalidSegment = segment(10)
      segmentValidator.isValid(invalidSegment, Mock.Hash("The real good hash")) mustBe false
    }

    "reject a segment with a single block, that is invalid" in {
      val block =
        validBlock.copy(signature = Signature.sign(validBlock.body.copy(delta = Nil), validSigningKey))
      segmentValidator.isValid(block :: Nil, validHash) mustBe false
    }
  }


  // HELPERS

  private def block(keys: List[KeyPair], j: Int, previous: Option[Block[String]] = None): Block[String] = {
    val ts = TimeSlot(j)
    val privateKeys = keys.map(_.PrivateKey)
    val signingKey: PrivateKey = privateKeys(ts.leader(keys.length) - 1)
    val body =
      BlockBody[String](
        hash = previous.map(b => Hash(b)).getOrElse(validHash),
        delta = List("ABC", "DEF"),
        timeSlot = ts,
        timeSlotSignature = Signature.sign(ts, signingKey)
      )
    Block(body, Signature.sign(body, signingKey))
  }

  private def segment(n: Int, j: Int = 1230): List[Block[String]] =
    n match {
      case negative if negative < 0 =>
        throw new Exception("Can't generate a segment with a negative ammount of Blocks")
      case 0 => Nil
      case _ =>
        val tail = segment(n - 1, j + 1)
        val head = block(keyPairs, j, tail.headOption)
        head :: tail
    }

  private def segval(n: Int) : (SegmentValidator, List[KeyPair]) = {
    val keys = List.fill(n)(KeyPair.gen)
    (new SegmentValidator(keys.map(_.PublicKey)), keys)
  }

  private val validHash: Hash[AnyBlock[String]] = Mock.Hash("good")
  private val (segmentValidator, keyPairs) = segval(7)
  private val privateKeys = keyPairs.map(_.PrivateKey)
  private val validBlock = block(keyPairs, 1230)
  private val validTimeSlot = validBlock.body.timeSlot
  private val validSigningKey: PrivateKey = privateKeys(validTimeSlot.leader(privateKeys.length) - 1)
  private val wrongKeys = KeyPair.gen()
}


