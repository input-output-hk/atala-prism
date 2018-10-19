package io.iohk.cef.data

trait DataItem {

  def id: String

  /**
    * Users/entities that witnessed this item and signed it
    * @return
    */
  def witnesses: Seq[Witness]

  /**
    * Users/entities with permission to eliminate this data item
    * @return
    */
  //TODO we will need to replace Seq with a simple boolean AST to better express ownership
  def owners: Seq[Owner]

  /**
    * Validates the data item and returns a specific error or nothing.
    * Does it make sense to return something else?
    * @return
    */
  def apply(): Either[DataItemError, Unit]
}
