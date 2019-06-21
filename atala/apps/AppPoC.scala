package atala.apps

import atala.logging._

import scala.concurrent.duration._
import scala.io.StdIn.readLine

import scala.concurrent.ExecutionContext.Implicits.global

object AppPoC {

  def transactionExecutor(accum: Map[Int, String], tx: (Int, String)): Option[Map[Int, String]] = {
    val (key, value) = tx
    if (accum.contains(key) && accum(key) != value) None
    else Some(accum + tx)
  }

  def printCommand(): Unit = {
    val fr = cluster.ask(())
    val (i, r) = scala.concurrent.Await.result(fr, Duration.Inf)
    println()
    println(s"State of server $i:")
    r.toList
      .sortBy(_._1)
      .foreach { case (k, v) => println(s"$k: $v") }
    println()
  }

  def printAllCommand(): Unit = {
    val fr = cluster.askAll(())
    val l = scala.concurrent.Await.result(fr, Duration.Inf)
    for { (i, r) <- l } {
      println()
      println(s"State of server $i:")
      r.toList
        .sortBy(_._1)
        .foreach { case (k, v) => println(s"$k: $v") }
      println()
    }
  }

  def ask[T](label: String, f: String => Option[T]): T = {
    while (true) {
      Console.print(s"$label > ")
      Console.flush
      readLine() match {
        case "exit" =>
          cluster.shutdown()
          sys.exit(1)
        case "print" => printCommand()
        case "printAll" => printAllCommand()
        case text =>
          f(text) match {
            case Some(t) => return t
            case None => println(s"\nNot a valid $label\n")
          }
      }
    }
    throw new Exception("Impossible")
  }

  import io.iohk.decco.auto._

  val cluster =
    Cluster[Map[Int, String], (Int, String), Unit, Map[Int, String]](7, 14, Map.empty)((s, _) => s, transactionExecutor)

  def main(args: Array[String]): Unit = {

    cluster.run()

    println(
      """|
         |COMMANDS:
         |  At any point:
         |    - exit
         |      exits the application
         |    - print
         |      prints the view of the world that one of the servers has in its blockchain
         |
         |  When asked for 'index':
         |    Introduce an integer value and the next 'value' is going to be stored at that index in the blockchain
         |
         |  When asked for 'value':
         |    Introduce any string except 'print' or 'exit'. That string is going to be stored at the provided index in the blockchain
         |""".stripMargin
    )

    while (true) {
      val index = ask[Int]("index", s => util.Try(s.toInt).toOption)
      val message = ask[String]("value", Option.apply)
      (index, message) sendTo cluster
      Thread.sleep(10)
    }
  }
}
