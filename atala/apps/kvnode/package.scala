package atala.apps

import atala.logging._

package object kvnode {

  type Tx = (Int, String)
  type S = Map[Int, String]

  val defaultState: S = Map.empty

  implicit def MapLoggable[A, B]: Loggable[Map[A, B]] = Loggable.gen(_.toString)

}
