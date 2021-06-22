package db.migration

import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.utils.Using.using
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

import java.sql.ResultSet

class V19__public_keys extends BaseJavaMigration {

  override def migrate(context: Context): Unit = {
    using(context.getConnection.prepareStatement("SELECT did_suffix, key_id, x, y FROM public_keys")) { select =>
      val rows: ResultSet = select.executeQuery()

      if (rows.next())
        loop(rows, context)

    }
  }

  def loop(row: ResultSet, context: Context): Unit = {

    val did_suffix = row.getString("did_suffix")
    val key_id = row.getString("key_id")

    val x = row.getBytes("x")
    val y = row.getBytes("y")

    val compressedX = EC.toPublicKey(x, y).getCompressed

    using(
      context.getConnection
        .prepareStatement("UPDATE public_keys SET xCompressed = ? WHERE did_suffix = ? AND key_id = ?")
    ) { update =>
      update.setBytes(1, compressedX)
      update.setString(2, did_suffix)
      update.setObject(3, key_id)
      update.execute()
    }

    if (row.next())
      loop(row, context)
    else ()

  }

}
