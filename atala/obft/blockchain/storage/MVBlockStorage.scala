package atala.obft.blockchain.storage

import java.nio.ByteBuffer
import java.nio.file.Path

import atala.obft.blockchain.models.Block
import io.iohk.decco.BufferInstantiator.global.HeapByteBuffer
import io.iohk.decco.Codec
import io.iohk.decco.auto._
import io.iohk.multicrypto._
import org.h2.mvstore.`type`.DataType
import org.h2.mvstore.{MVMap, MVStore, WriteBuffer}

class MVBlockStorage[Tx](storageFile: Path)(implicit codec: Codec[Tx]) extends BlockStorage[Tx] {

  import MVBlockStorage._

  private val tableId = "blocks"

  private val storage: MVStore = new MVStore.Builder()
    .fileName(storageFile.toAbsolutePath.toString)
    .open()

  private val table: MVMap[String, Block[Tx]] = {
    val map = new MVMap.Builder[String, Block[Tx]]()
      .valueType(new ByteBufferDataType[Block[Tx]])

    storage.openMap(tableId, map)
  }

  override def get(hash: Hash): Option[Block[Tx]] = {
    val nullable = table.get(hash.toCompactString())
    Option(nullable)
  }

  override def put(hash: Hash, block: Block[Tx]): Unit = {
    table.put(hash.toCompactString(), block)
  }

  override def remove(hash: Hash): Unit = {
    table.remove(hash.toCompactString())
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
