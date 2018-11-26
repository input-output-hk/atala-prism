package io.iohk.cef.utils.mv
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import io.iohk.cef.codecs.nio._
import org.h2.mvstore.{MVMap, MVStore}

import scala.collection.JavaConverters._

class MVTables(storageFile: Path) {
  private val storage: MVStore = new MVStore.Builder().fileName(storageFile.toAbsolutePath.toString).open()

  private val tables = new ConcurrentHashMap[String, Any]().asScala

  def table[T](tableId: String, codec: NioEncDec[T]): MVMap[String, T] = {
    val mapBuilder: MVMap.Builder[String, T] =
      new MVMap.Builder[String, T]().valueType(new ByteBufferDataType[T](codec))
    tables.getOrElseUpdate(tableId, storage.openMap(tableId, mapBuilder)).asInstanceOf[MVMap[String, T]]
  }

  def updatingTable[T](tableId: String, codec: NioEncDec[T])(block: MVMap[String, T] => Any): Unit = {
    try {
      block(table(tableId, codec))
    } finally {
      storage.commit()
      println(s"Committed.")
    }
  }
}
