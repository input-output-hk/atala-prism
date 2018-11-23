package io.iohk.cef.utils.mv
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.data.TableId
import org.h2.mvstore.{MVMap, MVStore}

import scala.collection.JavaConverters._

class MVTables(storageFile: Path) {
  private val storage: MVStore = new MVStore.Builder().fileName(storageFile.toAbsolutePath.toString).open()

  private val tables = new ConcurrentHashMap[TableId, MVMap[String, ByteBuffer]]().asScala

  def table(tableId: TableId): MVMap[String, ByteBuffer] = {
    tables.getOrElseUpdate(tableId, storage.openMap(tableId))
  }
}
