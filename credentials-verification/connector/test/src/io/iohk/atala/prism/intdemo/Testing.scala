package io.iohk.atala.prism.intdemo

import io.circe.{ACursor, Decoder, HCursor}
import org.mockito.Mockito
import org.mockito.Mockito.timeout
import org.mockito.verification.{VerificationMode, VerificationWithTimeout}

private[intdemo] object Testing {

  /**
    * Usage: {{{ verify(mock, eventually.times(1)).someMethod() }}}
    */
  def eventually: VerificationWithTimeout = timeout(100)

  /**
    * Verifies a method has never been called after an interval of 100 millis.
    * Usage: {{{ verify(mock, neverEver).someMethod() }}}
    */
  def neverEver: VerificationMode = Mockito.after(100).never

  /**
    * Returns the contents of the given resource file located in {@code intdemo}.
    */
  def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"intdemo/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }

  /**
    * Extract fields from JSON using dot notation names.
    * e.g. {{{ cursor.jsonStr("a.b.c") }}}
    * @param cursor an HCursor
    */
  implicit class CirceFieldAccess(cursor: HCursor) {
    import org.scalatest.EitherValues._
    def jsonStr(dotName: String): String = jsonVal[String](dotName)
    def jsonArr(dotName: String): List[String] = jsonVal[List[String]](dotName)
    def jsonNum[T: Numeric: Decoder](dotName: String): T = jsonVal[T](dotName)

    def jsonVal[T: Decoder](dotName: String): T = {
      @annotation.tailrec
      def loop(l: List[String], ac: ACursor): T = {
        l match {
          case Nil =>
            ac.as[T].right.value
          case h :: _ =>
            loop(l.tail, ac.downField(h))
        }
      }
      val l = dotName.split('.').toList
      loop(l.tail, cursor.downField(l.head))
    }
  }
}
