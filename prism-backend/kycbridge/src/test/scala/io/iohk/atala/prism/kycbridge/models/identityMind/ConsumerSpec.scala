package io.iohk.atala.prism.kycbridge.models.identityMind

import io.circe.parser
import io.circe.generic.auto._

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

class ConsumerSpec extends AnyWordSpec with Matchers {
  "Consumer" should {
    "be decodable from the JSON - GetConsumerResponse" in new Fixtures {
      parser.decode[GetConsumerResponse](getConsumerResponseJson) mustBe Right(getConsumerResponse)
    }

    "be accessible by data fields" in new Fixtures {
      getConsumerResponse.getDataField("dv:1") mustBe Some(
        EdnaScoreCardEntry(Some("dv:1"), Some("[Fired] dv:1(true) = true"))
      )
      getConsumerResponse.getDataField("dv:10") mustBe Some(EdnaScoreCardEntry(Some("dv:10"), Some("false")))
      getConsumerResponse.getDataField("dv:100") mustBe None
    }

    "be decodable from the JSON - AttributesResponse" in new Fixtures {
      parser.decode[AttributesResponse](attributesRequestJson) mustBe Right(attributesResponse)
    }
  }

  trait Fixtures extends ConsumerFixtures {
    val getConsumerResponseJson =
      """
      |{
      |  "ednaScoreCard": {
      |    "sc": [],
      |    "etr": [
      |      {
      |        "test": "dv:7",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:6",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:5",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:4",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:3",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "true"
      |      },
      |      {
      |        "test": "dv:2",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:1",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "fired": true,
      |        "condition": {
      |          "right": true,
      |          "left": "dv:1",
      |          "operator": "eq",
      |          "type": "info"
      |        },
      |        "details": "[Fired] dv:1(true) = true"
      |      },
      |      {
      |        "test": "dv:1",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "fired": false,
      |        "condition": {
      |          "right": false,
      |          "left": "dv:1",
      |          "operator": "eq",
      |          "type": "info"
      |        },
      |        "details": "dv:1(true) = false"
      |      },
      |      {
      |        "test": "dv:1",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "true"
      |      },
      |      {
      |        "test": "ed:32",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "fired": false,
      |        "condition": {
      |          "right": true,
      |          "left": "ed:32",
      |          "operator": "eq",
      |          "type": "info"
      |        },
      |        "details": "ed:32(false) = true"
      |      },
      |      {
      |        "test": "dv:0",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "true"
      |      },
      |      {
      |        "test": "dv:19",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "Identification Card"
      |      },
      |      {
      |        "test": "dv:10",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:20",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "true"
      |      },
      |      {
      |        "test": "dv:18",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "POL"
      |      },
      |      {
      |        "test": "dv:17",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "2022-05-15"
      |      },
      |      {
      |        "test": "dv:16",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "ZZC003483"
      |      },
      |      {
      |        "test": "dv:15",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "1985-07-12"
      |      },
      |      {
      |        "test": "dv:14",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "Address could NOT be extracted from the document."
      |      },
      |      {
      |        "test": "dv:24",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:13",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "MARIUSZ BOHDAN FIKUS"
      |      },
      |      {
      |        "test": "dv:9",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:12",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "ACUANT"
      |      },
      |      {
      |        "test": "dv:8",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      },
      |      {
      |        "test": "dv:11",
      |        "ts": 1625655704319,
      |        "stage": "1",
      |        "details": "false"
      |      }
      |    ],
      |    "er": {
      |      "profile": "assureid",
      |      "reportedRule": {
      |        "description": "",
      |        "details": "[Fired] dv:1(true) = true",
      |        "resultCode": "DENY",
      |        "ruleId": 30095,
      |        "testResults": [
      |          {
      |            "test": "dv:1",
      |            "ts": 1625655704319,
      |            "stage": "1",
      |            "fired": true,
      |            "condition": {
      |              "right": true,
      |              "left": "dv:1",
      |              "operator": "eq",
      |              "type": "info"
      |            },
      |            "details": "[Fired] dv:1(true) = true"
      |          }
      |        ],
      |        "name": "Identity (Reverse)"
      |      }
      |    }
      |  },
      |  "mtid": "f2b6e59ba27e443dabdfd8c0aaced8d3",
      |  "state": "D",
      |  "rcd": "",
      |  "tid": "f2b6e59ba27e443dabdfd8c0aaced8d3"
      |}
      """.stripMargin

    val attributesRequestJson =
      """
      |{
      |  "progress": "NEW",
      |  "applicationId": "f2b6e59ba27e443dabdfd8c0aaced8d3",
      |  "clientData": {
      |    "aggregatedAttributes": {
      |      "smid": "assureid",
      |      "stage": 1,
      |      "docType": "PP",
      |      "profile": "assureid",
      |      "tti": 1625655673000,
      |      "ccy": "USD",
      |      "man": "test9",
      |      "applicationId": "f2b6e59ba27e443dabdfd8c0aaced8d3",
      |      "docCountry": "PL"
      |    }
      |  },
      |  "priority": "NORMAL"
      |}
      """.stripMargin
  }
}
