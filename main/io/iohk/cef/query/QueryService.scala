package io.iohk.cef.query

/** Public API for the **internal** access to perform a query */
trait QueryService[QE <: QueryEngine, Q <: Query[QE]] {
  protected val engine: QE
  def perform(query: Q): query.Response = Query.performer(query, engine)
}
