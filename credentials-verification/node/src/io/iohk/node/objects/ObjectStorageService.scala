package io.iohk.node.objects

import scala.util.control.NonFatal
import io.iohk.node.services.models._
import io.iohk.node.services.BinaryOps

trait ObjectStorageService {

  import ObjectStorageService._

  /**
    * Store the object identified by id, overwriting it if exists.
    *
    * @param id the object identifier
    * @param data the data to store
    */
  def put(id: ObjectId, data: Array[Byte]): Unit

  /**
    * Find an object by its id.
    *
    * @param id the object identifier
    * @return the object data if it was found
    */
  def get(id: ObjectId): Option[Array[Byte]]

  def putAtalaObject(atalaObjectId: AtalaObjectId, data: Array[Byte]): Unit
}

object ObjectStorageService {

  type ObjectId = String

  def apply(): ObjectStorageService = {
    val binaryOps = BinaryOps()
    new FileBased(os.pwd / ".node", binaryOps)
  }

  class FileBased(baseDirectory: os.Path, binaryOps: BinaryOps) extends ObjectStorageService {
    override def put(id: ObjectId, data: Array[Byte]): Unit = {
      val path = baseDirectory / id
      os.write.over(path, data, createFolders = true)
    }

    override def get(id: ObjectId): Option[Array[Byte]] = {
      val path = baseDirectory / id
      try {
        val data = os.read.bytes(path)
        Some(data)
      } catch {
        case NonFatal(_) => None
      }
    }

    override def putAtalaObject(atalaObjectId: AtalaObjectId, data: Array[Byte]): Unit = {
      val objectId = binaryOps.convertBytesToHex(atalaObjectId)
      put(objectId, data)
    }
  }
}
