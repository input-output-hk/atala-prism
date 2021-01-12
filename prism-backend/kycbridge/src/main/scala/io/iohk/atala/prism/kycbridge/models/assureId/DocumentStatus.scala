package io.iohk.atala.prism.kycbridge.models.assureId

import enumeratum.{DoobieEnum, Enum, EnumEntry}

sealed abstract class DocumentStatus(value: String) extends EnumEntry {
  override def entryName: String = value
}
object DocumentStatus extends Enum[DocumentStatus] with DoobieEnum[DocumentStatus] {
  lazy val values = findValues

  final case object None extends DocumentStatus("NONE")
  final case object Classified extends DocumentStatus("CLASSIFIED")
  final case object Complete extends DocumentStatus("COMPLETE")
  final case object Error extends DocumentStatus("ERROR")

  def fromInt(value: Int): Either[IllegalArgumentException, DocumentStatus] = {
    value match {
      case value if value == 0 => Right(None)
      case value if value == 1 => Right(Classified)
      case value if value == 2 => Right(Complete)
      case value if value == 3 => Right(Error)
      case other => Left(new IllegalArgumentException(s"Invalid document status: $other"))
    }
  }
}
