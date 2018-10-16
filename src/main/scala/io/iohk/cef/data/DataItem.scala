package io.iohk.cef.data
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError

trait DataItem {

  /**
    * Users/entities that whitnessed this item and signed it
    * @return
    */
  def whitnesses: Seq[(SigningPublicKey, Signature)]

  /**
    * Users/entities with permission to eliminate this data item
    * @return
    */
  def owners: Seq[SigningPublicKey]

  /**
    * Validates the data item and returns a specific error or nothing.
    * Does it make sense to return something else?
    * @return
    */
  def validate: Either[ApplicationError, Unit]
}
