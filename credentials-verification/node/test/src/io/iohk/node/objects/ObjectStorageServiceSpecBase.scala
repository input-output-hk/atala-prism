package io.iohk.node.objects

import java.nio.file.Files

import io.iohk.node
import io.iohk.node.services.BinaryOps
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}
import os.Path

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.io.Directory

abstract class ObjectStorageServiceSpecBase(val storageClassName: String) extends WordSpec with MustMatchers {

  protected def createStorage: ObjectStorageService

  implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 10.millis)

  val exampleObjectId = "38a5dfa3ec07f08e8e1788d1d567359a7ed95b0e354953cf0222e0fea1872a7e"
  val otherObjectId = "38a5dfa3ec07f08e8e1788d1d567359a7ed95b0e354953cf0222e00feca1beef"
  val exampleData = "test data".getBytes

  storageClassName should {
    "fetch stored object" in {
      val storage = createStorage
      storage.put(exampleObjectId, exampleData).futureValue
      storage.get(exampleObjectId).futureValue.value must contain theSameElementsAs exampleData
    }

    "return empty Option if key not found" in {
      val storage = createStorage
      storage.get(otherObjectId).futureValue mustBe None
    }
  }
}

class InMemoryObjectStorageServiceSpec extends ObjectStorageServiceSpecBase("ObjectStorageService.InMemory") {
  def createStorage = new ObjectStorageService.InMemory()
}

class FileBasedObjectStorageServiceSpec
    extends ObjectStorageServiceSpecBase("ObjectStorageService.FileBased")
    with BeforeAndAfterAll {

  private val tempDir = Files.createTempDirectory("prism-node-tests").toFile
  private val binaryOps = BinaryOps()

  private def clearTempDir(): Unit = {
    if (tempDir.exists()) {
      new Directory(tempDir).deleteRecursively()
    }
  }

  override def createStorage: ObjectStorageService = {
    clearTempDir()
    new node.objects.ObjectStorageService.FileBased(Path(tempDir.getAbsolutePath), binaryOps)
  }

  override def afterAll(): Unit = {
    clearTempDir()
    super.afterAll()
  }
}
