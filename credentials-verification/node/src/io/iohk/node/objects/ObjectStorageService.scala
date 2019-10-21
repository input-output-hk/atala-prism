package io.iohk.node.objects

import scala.util.control.NonFatal

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
}

object ObjectStorageService {

  type ObjectId = String

  def apply(): ObjectStorageService = {
    new FileBased(os.pwd / ".node")
  }

  class FileBased(baseDirectory: os.Path) extends ObjectStorageService {
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
  }
}
