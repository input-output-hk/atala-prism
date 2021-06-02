package io.iohk.atala.prism.kycbridge.models.assureId

import io.circe.parser

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import java.time.LocalDateTime
import java.time.ZoneOffset

class DocumentSpec extends AnyWordSpec with Matchers {
  "Document" should {
    "be decodable from the JSON" in new Fixtures {
      parser.decode[Document](documentJson) mustBe Right(
        Document(
          instanceId = "a2f3e807-06a3-41e0-8fa2-d93875532272",
          biographic = Some(
            DocumentBiographic(
              age = Some(68),
              birthDate = Some(LocalDateTime.of(1952, 4, 7, 0, 0).toInstant(ZoneOffset.UTC)),
              expirationDate = Some(LocalDateTime.of(2029, 2, 11, 0, 0).toInstant(ZoneOffset.UTC)),
              fullName = Some("MARIUSZ BOHDAN FIKUS"),
              gender = Some(1),
              photo = Some(
                "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=Photo"
              ),
              unknownFields = Nil
            )
          ),
          classification = Some(
            DocumentClassification(
              `type` = Some(
                DocumentClassificationType(
                  `class` = Some(4),
                  className = Some("Identification Card"),
                  countryCode = Some("POL"),
                  issue = Some("2019"),
                  name = Some("Poland (POL) eIdentity Card"),
                  unknownFields = List(
                    "ClassCode",
                    "GeographicRegions",
                    "Id",
                    "IsGeneric",
                    "IssueType",
                    "IssuerCode",
                    "IssuerName",
                    "IssuerType",
                    "KeesingCode",
                    "Size",
                    "SupportedImages"
                  )
                )
              ),
              classificationDetails = Some(
                DocumentClassificationDetails(
                  back = None,
                  front = Some(DocumentClassificationDetailsFront(name = Some("Poland (POL) eIdentity Card")))
                )
              )
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
      )
    }

    "be decodable from the JSON with unknown fields information" in new Fixtures {
      val json =
        """
      |{
      |  "InstanceId": "a2f3e807-06a3-41e0-8fa2-d93875532272",
      |  "Biographic": {
      |    "Age": 68,
      |    "BirthDate": "/Date(-559699200000)/",
      |    "ExpirationDate": "/Date(1865462400000+0000)/",
      |    "FullName": "MARIUSZ BOHDAN FIKUS",
      |    "Gender": 1,
      |    "Photo": "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=Photo",
      |    "extra1": "value",
      |    "extra2": { "key": 1 }
      |  }
      |}""".stripMargin
      parser.decode[Document](json) mustBe Right(
        Document(
          instanceId = "a2f3e807-06a3-41e0-8fa2-d93875532272",
          biographic = Some(
            DocumentBiographic(
              age = Some(68),
              birthDate = Some(
                LocalDateTime.of(1952, 4, 7, 0, 0).toInstant(ZoneOffset.UTC)
              ), // Some("/Date(-559699200000+0000)/"), // TODO: Parse the date
              expirationDate = Some(
                LocalDateTime.of(2029, 2, 11, 0, 0).toInstant(ZoneOffset.UTC)
              ), // Some("/Date(1865462400000+0000)/"), // TODO: Parse the date
              fullName = Some("MARIUSZ BOHDAN FIKUS"),
              gender = Some(1),
              photo = Some(
                "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=Photo"
              ),
              unknownFields = List("extra1", "extra2")
            )
          ),
          classification = None,
          dataFields = None
        )
      )
    }
  }

  trait Fixtures {
    val documentJson =
      """
      |{
      |  "Alerts": [
      |    {
      |      "Actions": "The birth date on the document may have been misread.  Confirm that it is legible.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified that the birth date is valid.",
      |      "Disposition": "The birth date is valid",
      |      "FieldReferences": [
      |        "a31b60f1-4d42-4730-b078-7728f230531a"
      |      ],
      |      "Id": "73679ba3-edae-4aa9-be60-f621cf4c386b",
      |      "ImageReferences": [],
      |      "Information": "Verified that the birth date is valid, in the expected format, and occurs on or before the current date and not outside a reasonable range.",
      |      "Key": "Birth Date Valid",
      |      "Name": "Birth Date Valid",
      |      "RegionReferences": [],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "This test may fail if a document cannot be successfully classified as a supported document type.  This may occur if the document is fraudulent as some fraudulent documents differ so much from authentic documents that they will not be recognized as that type of document.  This may also occur if a valid document is significantly worn or damaged or if the document is of a new or different type that is not yet supported by the library.  The document should be examined manually.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified that the type of document is supported and is able to be fully authenticated.",
      |      "Disposition": "The document type is supported",
      |      "FieldReferences": [],
      |      "Id": "3c92e982-3443-4039-af18-aa5b63d02573",
      |      "ImageReferences": [],
      |      "Information": "Verified that the document is recognized as a supported document type and that the type of document can be fully authenticated.",
      |      "Key": "Document Classification",
      |      "Name": "Document Classification",
      |      "RegionReferences": [],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "The expiration date on the document may have been misread.  Confirm that it is legible and occurs on or after the current date.  Also confirm that the current date and time of the host computer is correctly set.",
      |      "DataFieldReferences": [],
      |      "Description": "Checked if the document is expired.",
      |      "Disposition": "The document has not expired",
      |      "FieldReferences": [
      |        "54229f9a-c010-4a11-96ba-eaea65bab88f"
      |      ],
      |      "Id": "eed74f4d-1ffa-4bc8-bc93-67ea94a828eb",
      |      "ImageReferences": [],
      |      "Information": "Verified that the document expiration date does not occur before the current date.",
      |      "Key": "Document Expired",
      |      "Name": "Document Expired",
      |      "RegionReferences": [],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "The expiration date on the document may have been misread.  Confirm that it is legible and occurs after the issue and birth dates.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified that the expiration date is valid.",
      |      "Disposition": "The expiration date is valid",
      |      "FieldReferences": [
      |        "54229f9a-c010-4a11-96ba-eaea65bab88f"
      |      ],
      |      "Id": "0a014c29-4300-4fce-87e2-73803784ae02",
      |      "ImageReferences": [],
      |      "Information": "Verified that the document expiration date is valid, in the expected format, and occurs after the issue and birth dates.",
      |      "Key": "Expiration Date Valid",
      |      "Name": "Expiration Date Valid",
      |      "RegionReferences": [],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "Check the visible (white) document image to verify the presence of the security feature.  Possible reasons this test may fail for a valid document may be that the document was moving during the capture, or the document may be excessively worn or damaged.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified the presence of a pattern on the visible image.",
      |      "Disposition": "A visible pattern was found",
      |      "FieldReferences": [],
      |      "Id": "22769216-02ad-4596-afc4-b8b1176f13ac",
      |      "ImageReferences": [],
      |      "Information": "Verified that a security feature in the visible spectrum is present and in an expected location on the document.",
      |      "Key": "Visible Pattern",
      |      "Name": "Visible Pattern",
      |      "RegionReferences": [
      |        "f1ebd55f-64b7-47b0-b76e-2a2fa2581cdf"
      |      ],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "Check the visible (white) document image to verify the presence of the security feature.  Possible reasons this test may fail for a valid document may be that the document was moving during the capture, or the document may be excessively worn or damaged.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified the presence of a pattern on the visible image.",
      |      "Disposition": "A visible pattern was found",
      |      "FieldReferences": [],
      |      "Id": "7a8319a9-0937-4a02-a982-d236deacbc64",
      |      "ImageReferences": [],
      |      "Information": "Verified that a security feature in the visible spectrum is present and in an expected location on the document.",
      |      "Key": "Visible Pattern",
      |      "Name": "Visible Pattern",
      |      "RegionReferences": [
      |        "d85702b7-e86c-4c2d-8ec1-a2bdc4b5b239"
      |      ],
      |      "Result": 1
      |    },
      |    {
      |      "Actions": "Check the visible (white) document image to verify the presence of the security feature.  Possible reasons this test may fail for a valid document may be that the document was moving during the capture, or the document may be excessively worn or damaged.",
      |      "DataFieldReferences": [],
      |      "Description": "Verified the presence of a pattern on the visible image.",
      |      "Disposition": "A visible pattern was found",
      |      "FieldReferences": [],
      |      "Id": "9926b7f5-c7d2-4cc1-815d-4a69c2939d82",
      |      "ImageReferences": [],
      |      "Information": "Verified that a security feature in the visible spectrum is present and in an expected location on the document.",
      |      "Key": "Visible Pattern",
      |      "Name": "Visible Pattern",
      |      "RegionReferences": [
      |        "4c2c29b0-603c-4c80-a94d-b49e0f70fbcb"
      |      ],
      |      "Result": 1
      |    }
      |  ],
      |  "AuthenticationSensitivity": 0,
      |  "Biographic": {
      |    "Age": 68,
      |    "BirthDate": "/Date(-559699200000+0000)/",
      |    "ExpirationDate": "/Date(1865462400000+0000)/",
      |    "FullName": "MARIUSZ BOHDAN FIKUS",
      |    "Gender": 1,
      |    "Photo": "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=Photo"
      |  },
      |  "Classification": {
      |    "ClassificationDetails": {
      |      "Back": null,
      |      "Front": {
      |        "Class": 4,
      |        "ClassCode": null,
      |        "ClassName": "Identification Card",
      |        "CountryCode": "POL",
      |        "GeographicRegions": [
      |          "EuropeMiddleEastAfrica"
      |        ],
      |        "Id": "01e694b7-9283-4b08-aace-a3bb2cd89275",
      |        "IsGeneric": false,
      |        "Issue": "2019",
      |        "IssueType": "eIdentity Card",
      |        "IssuerCode": "POL",
      |        "IssuerName": "Poland",
      |        "IssuerType": 1,
      |        "KeesingCode": null,
      |        "Name": "Poland (POL) eIdentity Card",
      |        "Size": 1,
      |        "SupportedImages": [
      |          {
      |            "Light": 0,
      |            "Side": 0
      |          },
      |          {
      |            "Light": 1,
      |            "Side": 0
      |          },
      |          {
      |            "Light": 2,
      |            "Side": 0
      |          },
      |          {
      |            "Light": 0,
      |            "Side": 1
      |          },
      |          {
      |            "Light": 1,
      |            "Side": 1
      |          },
      |          {
      |            "Light": 2,
      |            "Side": 1
      |          }
      |        ]
      |      }
      |    },
      |    "Mode": 0,
      |    "OrientationChanged": false,
      |    "PresentationChanged": false,
      |    "Type": {
      |      "Class": 4,
      |      "ClassCode": null,
      |      "ClassName": "Identification Card",
      |      "CountryCode": "POL",
      |      "GeographicRegions": [
      |        "EuropeMiddleEastAfrica"
      |      ],
      |      "Id": "01e694b7-9283-4b08-aace-a3bb2cd89275",
      |      "IsGeneric": false,
      |      "Issue": "2019",
      |      "IssueType": "eIdentity Card",
      |      "IssuerCode": "POL",
      |      "IssuerName": "Poland",
      |      "IssuerType": 1,
      |      "KeesingCode": null,
      |      "Name": "Poland (POL) eIdentity Card",
      |      "Size": 1,
      |      "SupportedImages": [
      |        {
      |          "Light": 0,
      |          "Side": 0
      |        },
      |        {
      |          "Light": 1,
      |          "Side": 0
      |        },
      |        {
      |          "Light": 2,
      |          "Side": 0
      |        },
      |        {
      |          "Light": 0,
      |          "Side": 1
      |        },
      |        {
      |          "Light": 1,
      |          "Side": 1
      |        },
      |        {
      |          "Light": 2,
      |          "Side": 1
      |        }
      |      ]
      |    }
      |  },
      |  "DataFields": [
      |    {
      |      "DataSource": 6,
      |      "Description": "The person's date of birth",
      |      "Id": "e677cace-6e6b-4db9-96e1-93e7501b0103",
      |      "IsImage": false,
      |      "Key": "VIZ Birth Date",
      |      "Name": "Birth Date",
      |      "RegionOfInterest": {
      |        "height": 14,
      |        "width": 90,
      |        "x": 5,
      |        "y": 0
      |      },
      |      "RegionReference": "b71ff9d8-4d93-494f-8e34-8610acc2b7a7",
      |      "Reliability": 0.55454546213150024,
      |      "Type": "datetime",
      |      "Value": "/Date(-559699200000+0000)/"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "A number that identifies a document",
      |      "Id": "2e0aabe5-5d80-4b57-b21d-67834bb34220",
      |      "IsImage": false,
      |      "Key": "VIZ Document Number",
      |      "Name": "Document Number",
      |      "RegionOfInterest": {
      |        "height": 18,
      |        "width": 101,
      |        "x": 12,
      |        "y": 3
      |      },
      |      "RegionReference": "100637ad-321d-4c4a-acb6-165e5922037b",
      |      "Reliability": 0.61699998378753662,
      |      "Type": "string",
      |      "Value": "ZZC003483"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "Date that the document will expire",
      |      "Id": "77ae6807-d3a1-4f33-bea8-e7971a77d2e2",
      |      "IsImage": false,
      |      "Key": "VIZ Expiration Date",
      |      "Name": "Expiration Date",
      |      "RegionOfInterest": {
      |        "height": 18,
      |        "width": 95,
      |        "x": 12,
      |        "y": 2
      |      },
      |      "RegionReference": "45723da9-73ae-4f43-9a23-8b153fceb243",
      |      "Reliability": 0.67818182706832886,
      |      "Type": "datetime",
      |      "Value": "/Date(1865462400000+0000)/"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "The person's full name (given name plus surname)",
      |      "Id": "80593985-b58c-4da0-be7c-13915dba8f8c",
      |      "IsImage": false,
      |      "Key": "VIZ Full Name",
      |      "Name": "Full Name",
      |      "RegionOfInterest": {
      |        "height": 0,
      |        "width": 0,
      |        "x": 0,
      |        "y": 0
      |      },
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Reliability": 0.8216666579246521,
      |      "Type": "string",
      |      "Value": "MARIUSZ BOHDAN FIKUS"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "The person's given name",
      |      "Id": "36ed2888-c2fb-4d6d-9b89-80ed6e276d25",
      |      "IsImage": false,
      |      "Key": "VIZ Given Name",
      |      "Name": "Given Name",
      |      "RegionOfInterest": {
      |        "height": 25,
      |        "width": 250,
      |        "x": 11,
      |        "y": 7
      |      },
      |      "RegionReference": "1ede9677-2edd-45d8-a4da-0b7a6aaed7b7",
      |      "Reliability": 0.85133332014083862,
      |      "Type": "string",
      |      "Value": "MARIUSZ BOHDAN"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "The person's nationality ",
      |      "Id": "80af543d-9307-4e16-9f84-df381ce3d528",
      |      "IsImage": false,
      |      "Key": "VIZ Nationality Name",
      |      "Name": "Nationality Name",
      |      "RegionOfInterest": {
      |        "height": 19,
      |        "width": 91,
      |        "x": 13,
      |        "y": 4
      |      },
      |      "RegionReference": "e6f1807a-8cf8-476c-a55b-173e5638d1f6",
      |      "Reliability": 0.67000001668930054,
      |      "Type": "string",
      |      "Value": "POLSKIE"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "A person's image",
      |      "Id": "73cfc3fa-df57-4e34-8158-389a643b7f8b",
      |      "IsImage": true,
      |      "Key": "VIZ Photo",
      |      "Name": "Photo",
      |      "RegionOfInterest": {
      |        "height": 0,
      |        "width": 0,
      |        "x": 0,
      |        "y": 0
      |      },
      |      "RegionReference": "5ec8cd37-91ef-4ae6-9b08-ab4c77a61b27",
      |      "Reliability": 0.9,
      |      "Type": "uri",
      |      "Value": "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=VIZ%20Photo"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "A single character (translated into M/F from other languages) that identifies the person's gender",
      |      "Id": "02ea7592-f7bb-4a39-b92c-047c4c577027",
      |      "IsImage": false,
      |      "Key": "VIZ Sex",
      |      "Name": "Sex",
      |      "RegionOfInterest": {
      |        "height": 14,
      |        "width": 13,
      |        "x": 12,
      |        "y": 0
      |      },
      |      "RegionReference": "c42fa55a-a571-4271-82a2-7185d3077cf5",
      |      "Reliability": 0.79499995708465576,
      |      "Type": "string",
      |      "Value": "M"
      |    },
      |    {
      |      "DataSource": 6,
      |      "Description": "The person's surname or family name",
      |      "Id": "8f3d7cc1-36b1-4e24-b50f-46a3f264b5c1",
      |      "IsImage": false,
      |      "Key": "VIZ Surname",
      |      "Name": "Surname",
      |      "RegionOfInterest": {
      |        "height": 22,
      |        "width": 81,
      |        "x": 9,
      |        "y": 8
      |      },
      |      "RegionReference": "77010f29-d49d-48e3-85b2-44c680074037",
      |      "Reliability": 0.8216666579246521,
      |      "Type": "string",
      |      "Value": "FIKUS"
      |    }
      |  ],
      |  "Device": {
      |    "HasContactlessChipReader": false,
      |    "HasMagneticStripeReader": false,
      |    "SerialNumber": null,
      |    "Type": {
      |      "Manufacturer": "String content",
      |      "Model": "String content",
      |      "SensorType": 0
      |    }
      |  },
      |  "EngineVersion": "2.16.16.243",
      |  "Fields": [
      |    {
      |      "DataFieldReferences": [
      |        "e677cace-6e6b-4db9-96e1-93e7501b0103"
      |      ],
      |      "DataSource": 6,
      |      "Description": "The person's date of birth",
      |      "Id": "a31b60f1-4d42-4730-b078-7728f230531a",
      |      "IsImage": false,
      |      "Key": "Birth Date",
      |      "Name": "Birth Date",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "datetime",
      |      "Value": "/Date(-559699200000+0000)/"
      |    },
      |    {
      |      "DataFieldReferences": [],
      |      "DataSource": 7,
      |      "Description": "The localized version of the document class of the document type",
      |      "Id": "f91e3a6c-9cef-463a-8e74-8438e3c97cde",
      |      "IsImage": false,
      |      "Key": "Document Class Name",
      |      "Name": "Document Class Name",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "Identification Card"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "2e0aabe5-5d80-4b57-b21d-67834bb34220"
      |      ],
      |      "DataSource": 6,
      |      "Description": "A number that identifies a document",
      |      "Id": "d975d303-4907-4b1b-b454-edf5753216df",
      |      "IsImage": false,
      |      "Key": "Document Number",
      |      "Name": "Document Number",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "ZZC003483"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "77ae6807-d3a1-4f33-bea8-e7971a77d2e2"
      |      ],
      |      "DataSource": 6,
      |      "Description": "Date that the document will expire",
      |      "Id": "54229f9a-c010-4a11-96ba-eaea65bab88f",
      |      "IsImage": false,
      |      "Key": "Expiration Date",
      |      "Name": "Expiration Date",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "datetime",
      |      "Value": "/Date(1865462400000+0000)/"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "80593985-b58c-4da0-be7c-13915dba8f8c"
      |      ],
      |      "DataSource": 6,
      |      "Description": "The person's full name (given name plus surname)",
      |      "Id": "bcf86e6b-e6dc-475c-a6c4-1eb1f273ea88",
      |      "IsImage": false,
      |      "Key": "Full Name",
      |      "Name": "Full Name",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "MARIUSZ BOHDAN FIKUS"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "36ed2888-c2fb-4d6d-9b89-80ed6e276d25"
      |      ],
      |      "DataSource": 6,
      |      "Description": "The person's given name",
      |      "Id": "9d4e5a9a-f60b-4177-bad8-b197ae692e0d",
      |      "IsImage": false,
      |      "Key": "Given Name",
      |      "Name": "Given Name",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "MARIUSZ BOHDAN"
      |    },
      |    {
      |      "DataFieldReferences": [],
      |      "DataSource": 7,
      |      "Description": "Abbreviated ID code of the state or country that issued the document",
      |      "Id": "0ac0767c-58ef-49ed-98ba-06fd65aca0d2",
      |      "IsImage": false,
      |      "Key": "Issuing State Code",
      |      "Name": "Issuing State Code",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "POL"
      |    },
      |    {
      |      "DataFieldReferences": [],
      |      "DataSource": 7,
      |      "Description": "Full name of the state or country that issued the document",
      |      "Id": "acba2cec-eda4-45da-a086-87098f8b9482",
      |      "IsImage": false,
      |      "Key": "Issuing State Name",
      |      "Name": "Issuing State Name",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "Poland"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "80af543d-9307-4e16-9f84-df381ce3d528"
      |      ],
      |      "DataSource": 6,
      |      "Description": "The person's nationality ",
      |      "Id": "ee2c516a-a9d1-42a2-add0-72ea2693d80c",
      |      "IsImage": false,
      |      "Key": "Nationality Name",
      |      "Name": "Nationality Name",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "POLSKIE"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "73cfc3fa-df57-4e34-8158-389a643b7f8b"
      |      ],
      |      "DataSource": 6,
      |      "Description": "A person's image",
      |      "Id": "3dbea41e-4e49-474f-b741-ceff882d5784",
      |      "IsImage": true,
      |      "Key": "Photo",
      |      "Name": "Photo",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "uri",
      |      "Value": "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Field/Image?key=Photo"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "02ea7592-f7bb-4a39-b92c-047c4c577027"
      |      ],
      |      "DataSource": 6,
      |      "Description": "A single character (translated into M/F from other languages) that identifies the person's gender",
      |      "Id": "3b9626f1-f937-4eb2-9186-33fe90dff692",
      |      "IsImage": false,
      |      "Key": "Sex",
      |      "Name": "Sex",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "M"
      |    },
      |    {
      |      "DataFieldReferences": [
      |        "8f3d7cc1-36b1-4e24-b50f-46a3f264b5c1"
      |      ],
      |      "DataSource": 6,
      |      "Description": "The person's surname or family name",
      |      "Id": "885d5162-75b4-478e-a0e1-41196c92536e",
      |      "IsImage": false,
      |      "Key": "Surname",
      |      "Name": "Surname",
      |      "RegionReference": "00000000-0000-0000-0000-000000000000",
      |      "Type": "string",
      |      "Value": "FIKUS"
      |    }
      |  ],
      |  "Images": [
      |    {
      |      "GlareMetric": null,
      |      "HorizontalResolution": 221,
      |      "Id": "45e89937-9b7b-4a37-809d-772525665356",
      |      "IsCropped": false,
      |      "IsTampered": false,
      |      "Light": 0,
      |      "MimeType": "image/jpeg",
      |      "SharpnessMetric": null,
      |      "Side": 0,
      |      "Uri": "https://preview.assureid.acuant.net/AssureIDService/Document/a2f3e807-06a3-41e0-8fa2-d93875532272/Image?side=Front&light=White",
      |      "VerticalResolution": 221
      |    }
      |  ],
      |  "InstanceId": "a2f3e807-06a3-41e0-8fa2-d93875532272",
      |  "LibraryVersion": "20.12.10.240",
      |  "ProcessMode": 2,
      |  "Regions": [
      |    {
      |      "DocumentElement": 3,
      |      "Id": "b71ff9d8-4d93-494f-8e34-8610acc2b7a7",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Birth Date",
      |      "Rectangle": {
      |        "height": 30,
      |        "width": 157,
      |        "x": 508,
      |        "y": 223
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "100637ad-321d-4c4a-acb6-165e5922037b",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Document Number",
      |      "Rectangle": {
      |        "height": 31,
      |        "width": 221,
      |        "x": 279,
      |        "y": 288
      |      }
      |    },
      |    {
      |      "DocumentElement": 4,
      |      "Id": "4a206bc2-27b9-4a95-b551-6f5953d5da13",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "eIdentity Card OVI",
      |      "Rectangle": {
      |        "height": 95,
      |        "width": 159,
      |        "x": 583,
      |        "y": 4
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "45723da9-73ae-4f43-9a23-8b153fceb243",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Expiration Date",
      |      "Rectangle": {
      |        "height": 34,
      |        "width": 179,
      |        "x": 281,
      |        "y": 338
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "d85702b7-e86c-4c2d-8ec1-a2bdc4b5b239",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Expiration Date Font",
      |      "Rectangle": {
      |        "height": 56,
      |        "width": 194,
      |        "x": 273,
      |        "y": 321
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "1ede9677-2edd-45d8-a4da-0b7a6aaed7b7",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Given Name",
      |      "Rectangle": {
      |        "height": 41,
      |        "width": 463,
      |        "x": 279,
      |        "y": 165
      |      }
      |    },
      |    {
      |      "DocumentElement": 4,
      |      "Id": "196faa30-6c12-42db-b7ab-75a1e3163a84",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Header",
      |      "Rectangle": {
      |        "height": 81,
      |        "width": 491,
      |        "x": 110,
      |        "y": 10
      |      }
      |    },
      |    {
      |      "DocumentElement": 4,
      |      "Id": "f1ebd55f-64b7-47b0-b76e-2a2fa2581cdf",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Header Left",
      |      "Rectangle": {
      |        "height": 129,
      |        "width": 154,
      |        "x": 25,
      |        "y": 3
      |      }
      |    },
      |    {
      |      "DocumentElement": 4,
      |      "Id": "4c2c29b0-603c-4c80-a94d-b49e0f70fbcb",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Header Microtext",
      |      "Rectangle": {
      |        "height": 42,
      |        "width": 498,
      |        "x": 128,
      |        "y": 55
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "e6f1807a-8cf8-476c-a55b-173e5638d1f6",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Nationality Name",
      |      "Rectangle": {
      |        "height": 37,
      |        "width": 226,
      |        "x": 279,
      |        "y": 223
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "826e9650-bcb0-40e0-97bf-af4fa62165bc",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Nationality Name Label",
      |      "Rectangle": {
      |        "height": 64,
      |        "width": 174,
      |        "x": 259,
      |        "y": 180
      |      }
      |    },
      |    {
      |      "DocumentElement": 2,
      |      "Id": "5ec8cd37-91ef-4ae6-9b08-ab4c77a61b27",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Photo",
      |      "Rectangle": {
      |        "height": 304,
      |        "width": 218,
      |        "x": 60,
      |        "y": 140
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "c42fa55a-a571-4271-82a2-7185d3077cf5",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Sex",
      |      "Rectangle": {
      |        "height": 38,
      |        "width": 86,
      |        "x": 502,
      |        "y": 271
      |      }
      |    },
      |    {
      |      "DocumentElement": 3,
      |      "Id": "77010f29-d49d-48e3-85b2-44c680074037",
      |      "ImageReference": "45e89937-9b7b-4a37-809d-772525665356",
      |      "Key": "Surname",
      |      "Rectangle": {
      |        "height": 43,
      |        "width": 462,
      |        "x": 280,
      |        "y": 108
      |      }
      |    }
      |  ],
      |  "Result": 1,
      |  "Subscription": {
      |    "DocumentProcessMode": 2,
      |    "Id": "856845dc-d78e-4b7e-92bc-1f592f827190",
      |    "IsActive": true,
      |    "IsDevelopment": true,
      |    "IsTrial": false,
      |    "Name": "dev_IOHK",
      |    "StorePII": true
      |  }
      |}
      """.stripMargin
  }
}
