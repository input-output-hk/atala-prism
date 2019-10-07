package io.iohk.node.bitcoin

import scala.language.higherKinds

import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.sttp.testing.SttpBackendStub
import io.iohk.node.bitcoin
import io.iohk.node.bitcoin.models._
import org.scalatest.EitherValues._
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

class BitcoinClientSpec extends WordSpec with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(30, Millis))

  "getBlock" should {
    "work" in {
      val blockhash = Blockhash.from("2dc74e01317c32509dd530ad466f1ec11582d95d7dba44fe069ca66c9f330464").value
      val response = readResource(blockPath(blockhash))

      val expected = Block.Canonical(
        BlockHeader(
          hash = blockhash,
          height = 40181,
          time = 1522836382L,
          previous = Blockhash.from("9d9816b341307d43aab6d1049ca51078b4656a3e31bd3abe7956b9df18f857e9")
        )
      )
      val client = newClient(response)
      val result = client.getBlock(blockhash).value.futureValue.right.value
      result must matchTo(expected)
    }
  }

  "getFullBlock" should {
    "work" in {
      val blockhash = Blockhash.from("2dc74e01317c32509dd530ad466f1ec11582d95d7dba44fe069ca66c9f330464").value
      val response = readResource(fullBlockPath(blockhash))

      val expected = Block.Full(
        BlockHeader(
          hash = blockhash,
          height = 40181,
          time = 1522836382L,
          previous = Blockhash.from("9d9816b341307d43aab6d1049ca51078b4656a3e31bd3abe7956b9df18f857e9")
        ),
        transactions = List(
          Transaction(
            TransactionId.from("f78011d83798ba5eb928bd6e79a35cfd8a03d3657044a32c24a1f8f99d5f0125").value,
            blockhash,
            List(Transaction.Output(Btc(0), 0, Transaction.OutputScript("nonstandard", "")))
          ),
          Transaction(
            TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
            blockhash,
            List(
              Transaction.Output(Btc(0), 0, Transaction.OutputScript("nonstandard", "")),
              Transaction.Output(
                Btc(1340),
                1,
                Transaction.OutputScript(
                  "pubkeyhash",
                  "OP_DUP OP_HASH160 750e6be359f1a26f6b16b0b3957e8f8270eb46f2 OP_EQUALVERIFY OP_CHECKSIG"
                )
              ),
              Transaction.Output(
                Btc(22.5),
                2,
                Transaction.OutputScript(
                  "pubkeyhash",
                  "OP_DUP OP_HASH160 a5ba7dc6c242607ef094c79ccfb8bdaddf831e97 OP_EQUALVERIFY OP_CHECKSIG"
                )
              )
            )
          ),
          Transaction(
            TransactionId.from("356f3fc21f4f55a6aa0cac21ceab47d79dc8a95b08c234c3b6721dc769900056").value,
            blockhash,
            List(
              Transaction.Output(
                Btc(0),
                0,
                Transaction.OutputScript(
                  "nulldata",
                  "OP_RETURN 586a55587938507a55464d78534c37594135767866574a587365746b354d5638676f 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99 1f60a6a385a4e5163ffef65dd873f17452bb0d9f89da701ffcc5a0f72287273c0571485c29123fef880d2d8169cfdb884bf95a18a0b36461517acda390ce4cf441"
                )
              ),
              Transaction.Output(
                Btc(1),
                1,
                Transaction.OutputScript(
                  "pubkeyhash",
                  "OP_DUP OP_HASH160 60653a6bcde4494bf67de03338d25ed9c576dd49 OP_EQUALVERIFY OP_CHECKSIG"
                )
              ),
              Transaction.Output(
                Btc(1249),
                2,
                Transaction.OutputScript(
                  "pubkeyhash",
                  "OP_DUP OP_HASH160 e92011c71d996b43432eff60d98bea648ed07d8e OP_EQUALVERIFY OP_CHECKSIG"
                )
              )
            )
          )
        )
      )
      val client = newClient(response)
      val result = client.getFullBlock(blockhash).value.futureValue.right.value
      result must matchTo(expected)
    }
  }

  private def newClient(response: String): BitcoinClient = {
    val config = bitcoin.api.rpc.RpcClient.Config("localhost", 0, "", "")
    val backend = SttpBackendStub.asynchronousFuture.whenAnyRequest.thenRespond(createRPCSuccessfulResponse(response))
    val client = new bitcoin.api.rpc.RpcClient(config)(backend, global)
    new bitcoin.BitcoinClient(client)
  }

  private def blockPath(blockhash: Blockhash): String = {
    s"bitcoin/blocks/${blockhash.string}"
  }

  private def fullBlockPath(blockhash: Blockhash): String = {
    s"bitcoin/full-blocks/${blockhash.string}"
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(resource).getLines().mkString("\n")
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }

  def createRPCSuccessfulResponse(result: String): String = {
    s"""
       |{
       |  "result": $result,
       |  "id": null,
       |  "error": null
       |}
     """.stripMargin
  }

  def createRPCErrorResponse(errorCode: Int, message: String): String = {
    s"""
       |{
       |  "result": null,
       |  "id": null,
       |  "error": {
       |    "code": $errorCode,
       |    "message": "$message"
       |  }
       |}
     """.stripMargin
  }
}
