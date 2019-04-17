package obft.logging

import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
  * Structured log facade for slf4j.
  */
trait AtalaLogging {

  protected val log = LoggerFactory.getLogger(this.getClass)

  // Groupping the logging methods to avoid polluting the namespace of extending class
  object logger {

    val info = genMethod(_.info(_))
    val trace = genMethod(_.trace(_))
    val debug = genMethod(_.debug(_))

    abstract class LoggingMethod(doLog: (Logger, String) => Unit) {
      type Tup[T] = (String, T)
      def apply(message: String): Unit = {
        val data = toStructuredLog(message, Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable](message: String, t1: Tup[T1]): Unit = {
        val data = toStructuredLog(message, t1.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable](message: String, t1: Tup[T1], t2: Tup[T2]): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable, T3: Loggable](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3]
      ): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: t3.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable, T3: Loggable, T4: Loggable](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4]
      ): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: t3.log :: t4.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable, T3: Loggable, T4: Loggable, T5: Loggable](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5]
      ): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable, T3: Loggable, T4: Loggable, T5: Loggable, T6: Loggable](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6]
      ): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: Nil)
        doLog(log, data)
      }
      def apply[T1: Loggable, T2: Loggable, T3: Loggable, T4: Loggable, T5: Loggable, T6: Loggable, T7: Loggable](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7]
      ): Unit = {
        val data = toStructuredLog(message, t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: Nil)
        doLog(log, data)
      }
      def apply[
          T1: Loggable,
          T2: Loggable,
          T3: Loggable,
          T4: Loggable,
          T5: Loggable,
          T6: Loggable,
          T7: Loggable,
          T8: Loggable
      ](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7],
          t8: Tup[T8]
      ): Unit = {
        val data =
          toStructuredLog(message, t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: t8.log :: Nil)
        doLog(log, data)
      }
      def apply[
          T1: Loggable,
          T2: Loggable,
          T3: Loggable,
          T4: Loggable,
          T5: Loggable,
          T6: Loggable,
          T7: Loggable,
          T8: Loggable,
          T9: Loggable
      ](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7],
          t8: Tup[T8],
          t9: Tup[T9]
      ): Unit = {
        val data = toStructuredLog(
          message,
          t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: t8.log :: t9.log :: Nil
        )
        doLog(log, data)
      }
      def apply[
          T1: Loggable,
          T2: Loggable,
          T3: Loggable,
          T4: Loggable,
          T5: Loggable,
          T6: Loggable,
          T7: Loggable,
          T8: Loggable,
          T9: Loggable,
          T10: Loggable
      ](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7],
          t8: Tup[T8],
          t9: Tup[T9],
          t10: Tup[T10]
      ): Unit = {
        val data = toStructuredLog(
          message,
          t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: t8.log :: t9.log :: t10.log :: Nil
        )
        doLog(log, data)
      }
      def apply[
          T1: Loggable,
          T2: Loggable,
          T3: Loggable,
          T4: Loggable,
          T5: Loggable,
          T6: Loggable,
          T7: Loggable,
          T8: Loggable,
          T9: Loggable,
          T10: Loggable,
          T11: Loggable
      ](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7],
          t8: Tup[T8],
          t9: Tup[T9],
          t10: Tup[T10],
          t11: Tup[T11]
      ): Unit = {
        val data = toStructuredLog(
          message,
          t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: t8.log :: t9.log :: t10.log :: t11.log :: Nil
        )
        doLog(log, data)
      }
      def apply[
          T1: Loggable,
          T2: Loggable,
          T3: Loggable,
          T4: Loggable,
          T5: Loggable,
          T6: Loggable,
          T7: Loggable,
          T8: Loggable,
          T9: Loggable,
          T10: Loggable,
          T11: Loggable,
          T12: Loggable
      ](
          message: String,
          t1: Tup[T1],
          t2: Tup[T2],
          t3: Tup[T3],
          t4: Tup[T4],
          t5: Tup[T5],
          t6: Tup[T6],
          t7: Tup[T7],
          t8: Tup[T8],
          t9: Tup[T9],
          t10: Tup[T10],
          t11: Tup[T11],
          t12: Tup[T12]
      ): Unit = {
        val data = toStructuredLog(
          message,
          t1.log :: t2.log :: t3.log :: t4.log :: t5.log :: t6.log :: t7.log :: t8.log :: t9.log :: t10.log :: t11.log :: t12.log :: Nil
        )
        doLog(log, data)
      }
    }

    private def genMethod(doLog: (Logger, String) => Unit): LoggingMethod = new LoggingMethod(doLog) {}
    private implicit class TExtensions[T](t: (String, T))(implicit loggable: Loggable[T]) {
      def log: (String, String) =
        (t._1, loggable.log(t._2))
    }

  }

  /**
    * Produces a JSON with the message details
    */
  private def toStructuredLog(message: String, ctx: List[(String, String)]): String = {
    val data = ("_message", message) :: ctx
    data
      .map {
        case (key, value) =>
          s""" "$key": "${value}" """
      }
      .mkString("{", ",", "}")
  }
}
