package io.iohk.atala.prism.intdemo

import io.circe.{ACursor, CursorOp, Decoder, FailedCursor, HCursor}
import org.mockito.Mockito
import org.mockito.verification.{VerificationMode, VerificationWithTimeout}
import org.scalatest.exceptions.TestFailedException

private[intdemo] object Testing {

  /** Usage: {{{verify(mock, eventually.times(1)).someMethod()}}}
    */
  def eventually: VerificationWithTimeout = Mockito.timeout(1000)

  /** Verifies a method has never been called after an interval of 100 millis. Usage:
    * {{{verify(mock, neverEver).someMethod()}}}
    */
  def neverEver: VerificationMode = Mockito.after(100).never

  /** Returns the contents of the given resource file located in {@code intdemo}.
    */
  def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"intdemo/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }

  /** Extract fields from JSON using dot notation names. e.g. {{{cursor.jsonStr("a.b.c")}}}
    * @param cursor
    *   an HCursor
    */
  implicit class CirceFieldAccess(cursor: HCursor) {
    import org.scalatest.OptionValues._
    import org.scalactic.source.Position

    def jsonStr(dotName: String): String = jsonVal[String](dotName)
    def jsonArr(dotName: String): List[String] = jsonVal[List[String]](dotName)

    def jsonVal[T: Decoder](dotName: String): T = {
      @annotation.tailrec
      def loop(l: List[String], ac: ACursor): T = {
        ac match {
          case failedCursor: FailedCursor =>
            val errMsg = s"Could not find ${CursorOp.opsToPath(failedCursor.history)} inside ${cursor.value}"
            val trace = Thread.currentThread.getStackTrace

            /** the exact index of StackTraceElement, in this case 4 is the original function that called jsonStr or
              * jsonArr, which is what we need in this case
              */
            val originalPosition = trace(4)
            throw new TestFailedException(
              _ => Some(errMsg),
              None,
              Position(
                fileName = originalPosition.getFileName,
                filePathname = originalPosition.getClassName,
                lineNumber = originalPosition.getLineNumber
              )
            )
          case _: HCursor =>
            l match {
              case Nil =>
                ac.as[T].toOption.value
              case h :: _ =>
                loop(l.tail, ac.downField(h))
            }
          case other =>
            throw new IllegalStateException("Unrecognized JSON cursor type: " + other.getClass)
        }

      }
      val l = dotName.split('.').toList
      loop(l.tail, cursor.downField(l.head))
    }
  }
}
