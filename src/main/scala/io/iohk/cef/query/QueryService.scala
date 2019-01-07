package io.iohk.query

/** Public API for the **internal** access to perform a query */
trait QueryService[QE <: QueryEngine, Q <: Query[QE]] {
  protected val engine: QE
  final def perform(query: Q): query.Response = Query.performer(query, engine)
}
