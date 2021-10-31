package io.iohk.atala.prism.utils

import cats.data.NonEmptyList
import cats.implicits._
import doobie.free.connection

/** Postgres has a limit of 32767 bind variables per statement. That's why all "find in" queries need to be split to not
  * exceed 32767 bind variables.
  */
object PostgresWhereInSplitter {

  private val POSTGRES_MAX_BIND_VALUES_COUNT = 32767

  /** Splits execution of SELECT ... WHERE ... IN ... into many queries and joins the results
    *
    * @param whereInValues
    *   list of values for the WHERE IN clauses
    * @param findQuery
    *   query ConnectionIO for chosen subset of values
    * @return
    *   joined results; it should be the same as findQuery(whereInValues)
    */
  def splitQueryExecution[A, B](
      whereInValues: List[A],
      findQuery: NonEmptyList[A] => doobie.ConnectionIO[List[B]]
  ): doobie.ConnectionIO[List[B]] =
    whereInValues
      .grouped(POSTGRES_MAX_BIND_VALUES_COUNT)
      .toList
      .traverse { values =>
        NonEmptyList.fromList(values).map(findQuery).getOrElse(connection.pure(List.empty[B]))
      }
      .map(_.flatten)

}
