package io.iohk.cef.query

/**
  * Base trait for a query, describing:
  *a) What type of information the query returns
  *b) How to perform the query
  *
  *@tparam QE  QueryEngine engine used to access the actual data to be queried
  */
trait Query[QE <: QueryEngine] {
  type Response
  protected def perform(queryEngine: QE): Response
}

object Query {

  /** Helper method allowing `QueryService` invoke the different queries,  without having to
      make the apply method on `Query` public **/
  def performer[QE <: QueryEngine, Q <: Query[QE]](query: Q, queryEngine: QE): query.Response =
    query.perform(queryEngine)
}
