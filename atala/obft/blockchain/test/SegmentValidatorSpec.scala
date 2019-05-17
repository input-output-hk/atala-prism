package atala.obft.blockchain
package test

// format: off

import atala.clock._
import atala.obft.blockchain.models._
import io.iohk.decco.auto._
import io.iohk.multicrypto._
import org.scalatest.{MustMatchers, WordSpec}

class SegmentValidatorSpec extends WordSpec with MustMatchers {

  "SegmentValidator.isValid(Block)" should {
    "accept a valid Block after the genesis" in {
      val validBlock = block(keyPairs, 1230, genesis)
      segmentValidator.isValid(validBlock, genesis) mustBe true
    }

    "accept a valid Block after a regular block" in {
      val previous = block(keyPairs, 1230, genesis)
      val validBlock = block(keyPairs, 1231, previous)
      segmentValidator.isValid(validBlock, previous) mustBe true
    }

    "reject a valid Block if its in the same time slot as the previous one" in {
      val previous = block(keyPairs, 1230, genesis)
      val invalidBlock = block(keyPairs, 1230, previous)
      segmentValidator.isValid(invalidBlock, previous) mustBe false
    }

    "reject a valid Block if its in before the time slot as the previous one" in {
      val previous = block(keyPairs, 1230, genesis)
      val invalidBlock = block(keyPairs, 1229, previous)
      segmentValidator.isValid(invalidBlock, previous) mustBe false
    }

    "reject a Block with an incorrect previous hash" in {
      val previous = block(keyPairs, 1230, genesis)
      val valid = block(keyPairs, 1231, previous)
      val invalid = valid.copy(body = valid.body.copy(previousHash = hash(genesis)))
      segmentValidator.isValid(invalid, valid) mustBe false
    }

    "reject a Block with the timeSlot signed by the wrong key" in {
      val validBlock: Block[String] = block(keyPairs, 1230, genesis)
      val wrongTimeSlotSignature = sign(TimeSlot(1230), wrongKeys.`private`)
      val invalid = validBlock.copy(body = validBlock.body.copy(timeSlotSignature = wrongTimeSlotSignature))

      segmentValidator.isValid(invalid, genesis) mustBe false
    }

    "reject a Block with the timeSlotSignature signing the incorrect timeSlot" in {
      val validBlock: Block[String] = block(keyPairs, 1230, genesis)
      val wrongTimeSlotSignature = sign(TimeSlot(1231), validSigningKey)
      val invalid = validBlock.copy(body = validBlock.body.copy(timeSlotSignature = wrongTimeSlotSignature))

      segmentValidator.isValid(invalid, genesis) mustBe false
    }

    "reject a Block whose body is signed with the wrong key" in {
      val validBlock: Block[String] = block(keyPairs, 1230, genesis)
      val wrongSignature = sign(validBlock.body, wrongKeys.`private`)
      val invalid = validBlock.copy(signature = wrongSignature)

      segmentValidator.isValid(invalid, genesis) mustBe false
    }

    "reject a Block whose signature is signing an incorrect body" in {
      val validBlock: Block[String] = block(keyPairs, 1230, genesis)
      val wrongSignature = sign(validBlock.body.copy(delta = List.empty[String]), validSigningKey)
      val invalid = validBlock.copy(signature = wrongSignature)

      segmentValidator.isValid(invalid, genesis) mustBe false
    }
  }

  "SegmentValidator.isValid(Segment)" should {

    "accept a valid segment" in {
      val validSegment = segment(10)
      segmentValidator.isValid(validSegment, genesis) mustBe true
    }

    "accept an empty segment" in {
      segmentValidator.isValid[String](Nil, genesis) mustBe true
    }

    "accept a segment with a single block, that is valid" in {
      segmentValidator.isValid(validBlock :: Nil, genesis) mustBe true
    }

    "reject a segment that contains an invalid block" in {
      val invalidSegment =
        segment(10)
          .zipWithIndex
          .map{
            case (b, 5) =>
              b.copy(signature = sign(b.body, wrongKeys.`private`))
            case (b, _) => b
          }
      segmentValidator.isValid(invalidSegment, genesis) mustBe false
    }

    "reject a segment that doesn't end with the requested hash" in {
      val genesis = GenesisBlock[String](List(generateSigningKeyPair().public))
      val invalidSegment = segment(10)
      segmentValidator.isValid(invalidSegment, genesis) mustBe false
    }

    "reject a segment with a single block, that is invalid" in {
      val block =
        validBlock.copy(signature = sign(validBlock.body.copy(delta = List.empty[String]), validSigningKey))
      segmentValidator.isValid(block :: Nil, genesis) mustBe false
    }
  }


  // HELPERS

  private def block(keys: List[SigningKeyPair], timeSlot: Int, previous: AnyBlock[String]): Block[String] = {
    val ts = TimeSlot(timeSlot)
    val privateKeys = keys.map(_.`private`)
    val publicKeys = keys.map(_.public)
    val signingKey: SigningPrivateKey = privateKeys(ts.leader(keys.length) - 1)
    val timeSlotSignature = sign(ts, signingKey)
    val body =
      BlockBody[String](
        previousHash = hash(previous),
        delta = List("ABC", "DEF"),
        timeSlot = ts,
        timeSlotSignature = timeSlotSignature
      )
    Block(body, sign(body, signingKey))
  }

  private def segment(n: Int, j: Int = 1230): List[Block[String]] =
    n match {
      case negative if negative < 0 =>
        throw new Exception("Can't generate a segment with a negative amount of Blocks")
      case 0 => Nil
      case _ =>
        val tail = segment(n - 1, j - 1)
        val head = block(keyPairs, j, tail.headOption.getOrElse(genesis))
        head :: tail
    }

  private def segval(n: Int) : (SegmentValidator, List[SigningKeyPair]) = {
    val keys = List.fill(n)(generateSigningKeyPair)
    (new SegmentValidator(keys.map(_.public)), keys)
  }

  private val (segmentValidator, keyPairs) = segval(7)
  private val privateKeys = keyPairs.map(_.`private`)
  private val genesis = GenesisBlock[String](keyPairs.map(_.public))
  private val validBlock: Block[String] = block(keyPairs,1230, genesis)
  private val validTimeSlot = validBlock.body.timeSlot
  private val validLeader: Int = validTimeSlot.leader(privateKeys.length)
  private val validSigningKey: SigningPrivateKey = privateKeys(validLeader - 1)
  private val wrongKeys = generateSigningKeyPair()
}
