package io.iohk.cef.data.query
import io.iohk.cef.data.Table
import io.iohk.cef.network.NetworkFacade

import scala.concurrent.duration.FiniteDuration

class QueryEngine(networkFacade: NetworkFacade, table: Table, timeout: FiniteDuration) {

  def process[T, U](query: Query[T, U]) = ???
}
