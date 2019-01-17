package io.iohk.cef.utils.mv

import java.nio.file.Path

import io.iohk.cef.codecs.nio._
import org.h2.mvstore.{MVMap, MVStore}

class MVTable[T](tableId: String, storageFile: Path, codec: NioCodec[T]) {

  private val storage: MVStore = new MVStore.Builder().fileName(storageFile.toAbsolutePath.toString).open()

  val table: MVMap[String, T] =
    storage.openMap(tableId, new MVMap.Builder[String, T]().valueType(new ByteBufferDataType[T](codec)))

  def update(block: MVMap[String, T] => Unit): Unit = {
    try {
      block(table)
    } finally {
      storage.commit()
    }
  }
}
