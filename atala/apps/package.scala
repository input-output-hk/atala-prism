package atala

import atala.logging._

package object apps {

  implicit def MapLoggable[A, B]: Loggable[Map[A, B]] = Loggable.gen(_.toString)

  implicit class StringExtraOps[Tx](val tx: Tx) extends AnyVal {

    def sendTo[S, Q, QR](server: Server[S, Tx, Q, QR]): Unit =
      server.receiveTransaction(tx)

    def sendTo[S, Q, QR](cluster: Cluster[S, Tx, Q, QR]): Unit =
      cluster.receiveTransaction(tx)
  }
}
