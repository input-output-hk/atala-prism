package io.iohk.atala.prism.node.objects

import org.scalatest.BeforeAndAfterEach
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, ListObjectsV2Request, S3Object}

import scala.jdk.CollectionConverters._

class S3ObjectStorageServiceSpec
    extends ObjectStorageServiceSpecBase("S3ObjectStorageServiceSpec")
    with BeforeAndAfterEach {

  import S3ObjectStorageService._

  val AWS_BUCKET = "atala-cvp"

  // be *VERY CAREFUL* editing it, wrong value might clean data in the S3 bucket
  val KEY_PREFIX = "node-test/"

  val region = Region.US_EAST_2

  private val testClient = S3Client.builder().region(region).build()
  private lazy val missingVariables =
    AWS_ENV_VARIABLES.filter(System.getenv(_) == null)
  private def s3Enabled = missingVariables.isEmpty

  override protected def createStorage: ObjectStorageService = {
    assume(
      missingVariables.isEmpty,
      s"You need ${missingVariables.mkString(", ")} set for this test"
    )

    cleanBucket()
    new S3ObjectStorageService(AWS_BUCKET, KEY_PREFIX, Some(region))
  }

  private def cleanBucket(): Unit = {
    assert(KEY_PREFIX.nonEmpty) // avoid cleaning all the data in the bucket

    def objectsStream(request: ListObjectsV2Request): LazyList[S3Object] = {
      val response = testClient.listObjectsV2(request)
      def tail =
        if (response.isTruncated) {
          val nextRequest = ListObjectsV2Request
            .builder()
            .bucket(AWS_BUCKET)
            .continuationToken(response.nextContinuationToken())
            .build()
          objectsStream(nextRequest)
        } else LazyList.empty

      response.contents().asScala.to(LazyList) ++ tail
    }

    val firstListRequest = ListObjectsV2Request
      .builder()
      .bucket(AWS_BUCKET)
      .prefix(KEY_PREFIX)
      .build()
    for (s3Object <- objectsStream(firstListRequest)) {
      assert(s3Object.key.startsWith(KEY_PREFIX)) // better be safe than sorry
      println(s"Deleting ${s3Object.key()}")
      testClient.deleteObject(
        DeleteObjectRequest
          .builder()
          .bucket(AWS_BUCKET)
          .key(s3Object.key())
          .build()
      )
    }
  }

  override def afterEach(): Unit = {
    super.afterEach()
    if (s3Enabled) {
      cleanBucket()
    }
  }
}
