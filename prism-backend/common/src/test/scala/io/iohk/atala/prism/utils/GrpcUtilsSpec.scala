package io.iohk.atala.prism.utils

import monix.execution.Scheduler.Implicits.global
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

class GrpcUtilsSpec extends AnyWordSpec with Matchers {

  "GrpcStreamsUtils" should {
    "create a working stream" in {
      // when
      val result: Seq[String] =
        GrpcStreamsUtils
          .createFs2Stream[String](stream => stream.onNext("test"))
          .interruptAfter(1.seconds)
          .compile
          .toList
          .runSyncUnsafe(1.minute)

      // then
      result mustBe Seq("test")
    }

    "create a failed stream" in {
      // when
      val result: Either[Throwable, String] =
        GrpcStreamsUtils
          .createFs2Stream[String](stream => stream.onError(new Throwable("")))
          .interruptAfter(1.seconds)
          .attempt
          .compile
          .fold[Either[Throwable, String]](Right("test"))((_, either) => either)
          .runSyncUnsafe(1.minute)

      // then
      result mustBe an[Left[Throwable, String]]
    }
  }

}
