package atala.apps.kvnode

import atala.clock.Clock
import atala.logging.Loggable
import atala.obft.{OuroborosBFT, NetworkMessage, Tick}
import atala.network.{OBFTNetworkInterface, OBFTPeerGroupNetworkInterface}
import atala.config._
import io.iohk.decco.Codec
import io.iohk.decco.auto._
import io.iohk.multicrypto._
import io.iohk.scalanet.peergroup.InetMultiAddress
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import atala.helpers.monixhelpers._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  println("Starting a KV-Node")

  pureconfig.loadConfig[KvConfig] match {
    case Right(config) =>
      start(config)
    case Left(error) =>
      println("FATAL. Couldn't load configuration due to:")
      println(error)
      println("STOPPING THE NODE NOW")
      sys.exit(1)
  }

  def start(configuration: KvConfig): Unit = {

    def transactionExecutor(accum: S, tx: Tx): Option[S] = {
      val (key, value) = tx
      if (accum.contains(key)) None
      else Some(accum + tx)
    }

    val task =
      for {
        controller <- ObftController[S, Tx, Unit, S](configuration.obft, defaultState)((s, _) => s, transactionExecutor)
        rest = Rest(configuration.restAddress, controller)
      } yield {
        controller.start()
        rest.start()
      }

    task.runAsync
  }

}
