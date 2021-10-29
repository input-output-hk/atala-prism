package io.iohk.atala.prism.node.objects

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait ObjectStorageService {

  import ObjectStorageService._

  /** Store the object identified by id, overwriting it if exists.
    *
    * @param id
    *   the object identifier
    * @param data
    *   the data to store
    */
  def put(id: ObjectId, data: Array[Byte]): Future[Unit]

  /** Find an object by its id.
    *
    * @param id
    *   the object identifier
    * @return
    *   the object data if it was found
    */
  def get(id: ObjectId): Future[Option[Array[Byte]]]
}

object ObjectStorageService {

  type ObjectId = String

  def apply()(implicit ec: ExecutionContext): ObjectStorageService = {
    new FileBased(os.pwd / ".node")
  }

  class InMemory extends ObjectStorageService {
    private var dataMap: Map[ObjectId, Array[Byte]] = Map.empty
    override def put(id: ObjectId, data: Array[Byte]): Future[Unit] = {
      dataMap += id -> data
      Future.successful(())
    }

    override def get(id: ObjectId): Future[Option[Array[Byte]]] = {
      Future.successful(dataMap.get(id))
    }
  }

  class FileBased(baseDirectory: os.Path)(implicit ec: ExecutionContext) extends ObjectStorageService {
    override def put(id: ObjectId, data: Array[Byte]): Future[Unit] = {
      val path = baseDirectory / id
      Future {
        os.write.over(path, data, createFolders = true)
      }
    }

    override def get(id: ObjectId): Future[Option[Array[Byte]]] = {
      val path = baseDirectory / id
      Future {
        val data = os.read.bytes(path)
        Some(data)
      } recover { case NonFatal(_) =>
        None
      }
    }
  }
}
