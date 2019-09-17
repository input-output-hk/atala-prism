package io.iohk.node.bitcoin

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe._
import io.circe.parser._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait BitcoinClient {
  def getLatestBlockhash: Future[Blockhash]
  def getBlock(blockhash: Blockhash): Future[Option[Block]]
  def getBlockhash(height: Int): Future[Option[Blockhash]]
}

object BitcoinClient {

  case class Config(host: String, port: Int, username: String, password: String)

  class DefaultImpl(config: Config)(implicit ec: ExecutionContext) extends BitcoinClient {

    private implicit val backend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

    private val server = sttp
      .post(Uri.apply(config.host, config.port))
      .header("Content-Type", "text/plain")
      .response(asString)
      .auth
      .basic(config.username, config.password)

    implicit val blockhashDecoder: Decoder[Blockhash] = implicitly[Decoder[String]].emapTry { string =>
      Try(Blockhash.from(string).getOrElse(throw new RuntimeException("Invalid blockhash")))
    }

    implicit val blockDecoder: Decoder[Block] =
      Decoder.forProduct4("hash", "height", "time", "previousblockhash")(Block.apply)

    override def getBlock(blockhash: Blockhash): Future[Option[Block]] = {
      val body = s"""{ "jsonrpc": "1.0", "method": "getblock", "params": ["${blockhash.string}"] }"""

      server
        .body(body)
        .send()
        .map { response =>
          getResult[Block](response).map(Option.apply).getOrElse(throw new RuntimeException("Failed to get block"))
        }
    }

    override def getLatestBlockhash: Future[Blockhash] = {
      val body = s"""
                    |{
                    |  "jsonrpc": "1.0",
                    |  "method": "getbestblockhash",
                    |  "params": []
                    |}
                    |""".stripMargin

      server
        .body(body)
        .send()
        .map { response =>
          getResult[String](response)
            .flatMap(Blockhash.from)
            .getOrElse(throw new RuntimeException("Failed to get latest blockhash"))
        }
    }

    override def getBlockhash(height: Int): Future[Option[Blockhash]] = {
      val body = s"""{ "jsonrpc": "1.0", "method": "getblockhash", "params": [$height] }"""

      server
        .body(body)
        .send()
        .map { response =>
          getResult[String](response)
            .flatMap(Blockhash.from)
            .map(Option.apply)
            .getOrElse(throw new RuntimeException("Failed to get latest blockhash"))
        }
    }
  }

  private def getResult[A: Decoder](response: Response[String]): Option[A] = {
    val maybe = Option(response)
      .filter(_.code == 200)
      .flatMap { r =>
        Try(r.unsafeBody).map(parse).toOption.flatMap(_.toOption)
      }
      .map { json =>
        json.hcursor.downField("result").as[A].getOrElse(throw new RuntimeException("Not found"))
      }

    maybe
  }
}
