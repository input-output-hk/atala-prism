package io.iohk.atala.prism.kycbridge.models.assureId

import java.time.{LocalDateTime, ZoneOffset}

trait DocumentFixtures {
  val document = Document(
    instanceId = "id",
    biographic = Some(
      DocumentBiographic(
        age = Some(30),
        birthDate = Some(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC)),
        expirationDate = Some(LocalDateTime.of(2025, 9, 5, 0, 0).toInstant(ZoneOffset.UTC)),
        fullName = Some("MARIUSZ BOHDAN FIKUS"),
        gender = Some(1),
        photo = Some("url"),
        unknownFields = List("test")
      )
    ),
    classification = Some(
      DocumentClassification(
        `type` = None,
        classificationDetails = None
      )
    ),
    dataFields = Some(
      List(
        DocumentDataField(
          key = Some("VIZ Birth Date"),
          name = Some("Birth Date"),
          value = Some("/Date(-559699200000+0000)/")
        ),
        DocumentDataField(
          key = Some("VIZ Document Number"),
          name = Some("Document Number"),
          value = Some("ZZC003483")
        ),
        DocumentDataField(
          key = Some("VIZ Expiration Date"),
          name = Some("Expiration Date"),
          value = Some("/Date(1865462400000+0000)/")
        ),
        DocumentDataField(
          key = Some("VIZ Full Name"),
          name = Some("Full Name"),
          value = Some("MARIUSZ BOHDAN FIKUS")
        ),
        DocumentDataField(
          key = Some("VIZ Given Name"),
          name = Some("Given Name"),
          value = Some("MARIUSZ BOHDAN")
        ),
        DocumentDataField(
          key = Some("VIZ Nationality Name"),
          name = Some("Nationality Name"),
          value = Some("POLSKIE")
        ),
        DocumentDataField(
          key = Some("VIZ Photo"),
          name = Some("Photo"),
          value = Some(
            "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=VIZ%20Photo"
          )
        ),
        DocumentDataField(
          key = Some("VIZ Sex"),
          name = Some("Sex"),
          value = Some("M")
        ),
        DocumentDataField(
          key = Some("VIZ Surname"),
          name = Some("Surname"),
          value = Some("FIKUS")
        )
      )
    )
  )
}
