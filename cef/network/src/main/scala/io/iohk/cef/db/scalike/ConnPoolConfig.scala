package io.iohk.cef.db.scalike

import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import scalikejdbc.config.DBs
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, JDBCSettings}

object ConnPoolConfig {
  val settings: JDBCSettings = DBs.readJDBCSettings('default)
  val dataSource: DataSource = {
    val ds = new HikariDataSource()
    ds.setDataSourceClassName(settings.driverName)
    ds.addDataSourceProperty("url", settings.url)
    ds.addDataSourceProperty("user", settings.user)
    ds.addDataSourceProperty("password", settings.password)
    ds
  }
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
}
