package db.migration

import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

import java.sql.ResultSet
import scala.util.{Failure, Success, Try, Using}

class V19__public_keys extends BaseJavaMigration {

  override def migrate(context: Context): Unit = {
    Try {
      val rows = context.getConnection.createStatement
        .executeQuery(
          "SELECT did_suffix, key_id, x, y FROM public_keys WHERE xCompressed is NULL"
        )
      if (rows.next())
        loop(rows, context)
    } match {
      case Failure(exception) =>
        exception.printStackTrace()
        throw new Exception("V19__public_keys migration failed")
      case Success(_) => println("V19__public_keys migration succeed")
    }
  }

  def loop(row: ResultSet, context: Context): Unit = {

    val did_suffix = row.getString("did_suffix")
    val key_id = row.getString("key_id")

    val x = row.getBytes("x")
    val y = row.getBytes("y")

    val compressedX: Array[Byte] =
      EC.toPublicKeyFromByteCoordinates(x, y).getEncodedCompressed

    Using(
      context.getConnection
        .prepareStatement(
          "UPDATE public_keys SET xCompressed = ? WHERE did_suffix = ? AND key_id = ?"
        )
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
