package io.iohk.atala.mirror.db

import doobie.free.connection.ConnectionIO
import doobie.implicits._

object ExampleDao {
  def test(): ConnectionIO[String] = {
    sql"SELECT 'test'".query[String].unique
  }
}
