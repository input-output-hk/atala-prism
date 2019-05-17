package atala

package object apps {

  implicit class StringExtraOps[Tx](val tx: Tx) extends AnyVal {

    def sendTo[S, Q, QR](server: Server[S, Tx, Q, QR]): Unit =
      server.receiveTransaction(tx)

    def sendTo[S, Q, QR](cluster: Cluster[S, Tx, Q, QR]): Unit =
      cluster.receiveTransaction(tx)
  }
}
