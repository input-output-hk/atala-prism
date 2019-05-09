package atala.obft.blockchain.storage

import java.nio.ByteBuffer
import java.nio.file.Path

import io.iohk.decco.BufferInstantiator.global.HeapByteBuffer
import io.iohk.decco.Codec
import io.iohk.decco.auto._
import io.iohk.multicrypto._
import atala.obft.blockchain.models.AnyBlock
import org.h2.mvstore.`type`.DataType
import org.h2.mvstore.{MVMap, MVStore, WriteBuffer}

class MVBlockStorage[Tx](storageFile: Path)(implicit codec: Codec[Tx]) extends BlockStorage[Tx] {

  import MVBlockStorage._

  private val tableId = "blocks"

  private val storage: MVStore = new MVStore.Builder()
    .fileName(storageFile.toAbsolutePath.toString)
    .open()

  private val table: MVMap[String, AnyBlock[Tx]] = {
    val map = new MVMap.Builder[String, AnyBlock[Tx]]()
      .valueType(new ByteBufferDataType[AnyBlock[Tx]])

    storage.openMap(tableId, map)
  }

  override def get(id: Hash): Option[AnyBlock[Tx]] = {
    val nullable = table.get(id.toCompactString())
    Option(nullable)
  }

  override def put(id: Hash, block: AnyBlock[Tx]): Unit = {
    table.put(id.toCompactString(), block)
  }

  override def remove(id: Hash): Unit = {
    table.remove(id.toCompactString())
  }
}

object MVBlockStorage {

  class ByteBufferDataType[T](implicit codec: Codec[T]) extends DataType {

    override def compare(a: Any, b: Any): Int = {
      if (!a.isInstanceOf[Ordered[T]]) {
        throw new UnsupportedOperationException("Stored type is not has no ordering defined")
      }
      a.asInstanceOf[Ordered[T]].compare(b.asInstanceOf[T])
    }

    override def getMemory(obj: Any): Int = {
      codec.encode(obj.asInstanceOf[T]).capacity()
    }

    override def write(buff: WriteBuffer, obj: Any): Unit = obj match {
      case x: T => buff.put(codec.encode(x))
      case _ => throw new RuntimeException(s"Unsupported object: $obj")
    }

    override def write(buff: WriteBuffer, obj: Array[AnyRef], len: Int, key: Boolean): Unit =
      (0 until len).foreach(i => write(buff, obj(i)))

    override def read(buff: ByteBuffer): AnyRef =
      codec
        .decode(buff)
        .getOrElse(throw new IllegalStateException("Decoding error in underlying storage"))
        .asInstanceOf[AnyRef]

    override def read(buff: ByteBuffer, obj: Array[AnyRef], len: Int, key: Boolean): Unit =
      (0 until len).foreach(i => obj(i) = read(buff))
  }
}
