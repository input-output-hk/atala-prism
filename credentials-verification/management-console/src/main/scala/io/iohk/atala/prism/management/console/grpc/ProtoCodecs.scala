package io.iohk.atala.prism.management.console.grpc

import java.time.LocalDate

import io.iohk.atala.prism.management.console.models.Contact
import io.iohk.atala.prism.protos.{common_models, console_models}
import io.scalaland.chimney.Transformer

object ProtoCodecs {

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  def toContactProto(contact: Contact): console_models.Contact = {
    console_models
      .Contact()
      .withContactId(contact.contactId.value.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withCreatedAt(contact.createdAt.toEpochMilli)
  }
}
