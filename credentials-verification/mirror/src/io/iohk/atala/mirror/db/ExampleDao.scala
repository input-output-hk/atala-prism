package io.iohk.atala.mirror.db

import doobie.free.connection.ConnectionIO
import doobie.implicits._

object ExampleDao {
  def test(): ConnectionIO[Boolean] = {
    sql"SELECT 1 = 1".query[Boolean].unique
  }
}
