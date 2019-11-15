package io.iohk.node

package object errors {
  sealed trait NodeError

  object NodeError {

    /** Error indicating lack of some value required for the operation
      *
      * @param tpe type of the value, e.g. "didSuffix" or "contract"
      * @param identifier identifier used to look for the value
      */
    case class UnknownValueError(tpe: String, identifier: String) extends NodeError
  }

}
