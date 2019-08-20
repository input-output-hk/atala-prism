package io.iohk.indy

object ExampleRunner {
  def main(args: Array[String]): Unit = {
    println("Indy Examples")
    println("Choose example")

    val examples: List[HasMain] = List(
      WriteDIDAndQueryVerKey,
      RotateKey,
      SaveSchemaAndCredentialDefinition,
      IssueCredential
    )

    for (item <- examples.zipWithIndex) {
      println(s"${item._2} - ${item._1}")
    }

    print("choose: ")
    val id = scala.io.StdIn.readInt()
    if (id >= 0 && id < examples.length) {
      examples(id).main(args)
    } else {
      println("Choose a valid item")
    }
  }
}
