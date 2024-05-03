package io.iohk.atala.prism.node.identity

case class DidMethodSpecificId(private val value: String) {
  val sections: Array[String] = value.split(":")

  override def toString: String = value
}

object DidMethodSpecificId {
  private val sectionRegex = "^[A-Za-z0-9_-]+$".r
  private val methodSpecificIdRegex = "^([A-Za-z0-9_-]*:)*[A-Za-z0-9_-]+$".r

  def fromString(string: String): DidMethodSpecificId = {
    require(
      methodSpecificIdRegex.matches(string),
      s"Does not conform to DID method-specific identifier format: '$string'"
    )

    DidMethodSpecificId(string)
  }

  def fromSections(sections: Array[String]): DidMethodSpecificId = {

    val allMatching = sections.forall(sectionRegex.matches(_))
    require(allMatching, s"One of the sections contains something other than ${sectionRegex.toString}")

    DidMethodSpecificId(sections.mkString(":"))
  }
}
