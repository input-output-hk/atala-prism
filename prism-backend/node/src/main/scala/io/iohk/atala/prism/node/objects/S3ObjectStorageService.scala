package io.iohk.atala.prism.node.objects

import java.util.concurrent.CompletionException

import io.iohk.atala.prism.node.objects.ObjectStorageService.ObjectId
import io.iohk.atala.prism.node.objects.S3ObjectStorageService.AWS_ENV_VARIABLES
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse,
  NoSuchKeyException,
  PutObjectRequest
}

import scala.concurrent.{Future, Promise}

object S3ObjectStorageService {
  val AWS_ENV_VARIABLES = Seq(
    "AWS_ACCESS_KEY_ID",
    "AWS_SECRET_ACCESS_KEY"
  )
}

class S3ObjectStorageService(
    bucket: String,
    keyPrefix: String,
    region: Option[Region] = None
) extends ObjectStorageService {
  require(keyPrefix.endsWith("/"), "Key prefix needs to end with /")
  private val missingVariables =
    AWS_ENV_VARIABLES.filter(System.getenv(_) == null)
  require(
    missingVariables.isEmpty,
    s"You need ${missingVariables.mkString(", ")} in order to use S3 storage"
  )

  val client = {
    val builder = S3AsyncClient.builder
    val builderWithRegion = region.fold(builder)(builder.region)
    builderWithRegion.build()
  }

  /** Store the object identified by id, overwriting it if exists.
    *
    * @param id
    *   the object identifier
    * @param data
    *   the data to store
    */
  override def put(id: ObjectId, data: Array[Byte]): Future[Unit] = {
    val request = PutObjectRequest
      .builder()
      .bucket(bucket)
      .key(keyPrefix + id)
      .build()

    val promise = Promise[Unit]()

    client
      .putObject(request, AsyncRequestBody.fromBytes(data))
      .whenComplete { (resp, err) =>
        if (resp != null) {
          promise.success(())
        } else {
          promise.failure(err)
        }
      }

    promise.future
  }

  /** Find an object by its id.
    *
    * @param id
    *   the object identifier
    * @return
    *   the object data if it was found
    */
  override def get(id: ObjectId): Future[Option[Array[Byte]]] = {
    val request = GetObjectRequest
      .builder()
      .bucket(bucket)
      .key(keyPrefix + id)
      .build()

    val promise = Promise[Option[Array[Byte]]]()

    client
      .getObject(
        request,
        new ByteArrayAsyncResponseTransformer[GetObjectResponse]()
      )
      .whenComplete { (resp, err) =>
        if (resp != null) {
          promise.success(Some(resp.asByteArray()))
        } else {
          err match {
            case ex: CompletionException =>
              ex.getCause match {
                case _: NoSuchKeyException =>
                  promise.success(None)
                case _ =>
                  promise.failure(ex)
              }
            case ex =>
              promise.failure(ex)
          }
        }
      }

    promise.future
  }
}
