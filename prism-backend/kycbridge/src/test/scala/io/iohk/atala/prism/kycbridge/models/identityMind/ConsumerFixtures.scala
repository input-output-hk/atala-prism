package io.iohk.atala.prism.kycbridge.models.identityMind

trait ConsumerFixtures {
  val getConsumerResponse = GetConsumerResponse(
    mtid = "f2b6e59ba27e443dabdfd8c0aaced8d3",
    state = ConsumerResponseState.Deny,
    ednaScoreCard = EdnaScoreCard(
      etr = List(
        EdnaScoreCardEntry(Some("dv:7"), Some("false")),
        EdnaScoreCardEntry(Some("dv:6"), Some("false")),
        EdnaScoreCardEntry(Some("dv:5"), Some("false")),
        EdnaScoreCardEntry(Some("dv:4"), Some("false")),
        EdnaScoreCardEntry(Some("dv:3"), Some("true")),
        EdnaScoreCardEntry(Some("dv:2"), Some("false")),
        EdnaScoreCardEntry(Some("dv:1"), Some("[Fired] dv:1(true) = true")),
        EdnaScoreCardEntry(Some("dv:1"), Some("dv:1(true) = false")),
        EdnaScoreCardEntry(Some("dv:1"), Some("true")),
        EdnaScoreCardEntry(Some("ed:32"), Some("ed:32(false) = true")),
        EdnaScoreCardEntry(Some("dv:0"), Some("true")),
        EdnaScoreCardEntry(Some("dv:19"), Some("Identification Card")),
        EdnaScoreCardEntry(Some("dv:10"), Some("false")),
        EdnaScoreCardEntry(Some("dv:20"), Some("true")),
        EdnaScoreCardEntry(Some("dv:18"), Some("POL")),
        EdnaScoreCardEntry(Some("dv:17"), Some("2022-05-15")),
        EdnaScoreCardEntry(Some("dv:16"), Some("ZZC003483")),
        EdnaScoreCardEntry(Some("dv:15"), Some("1985-07-12")),
        EdnaScoreCardEntry(Some("dv:14"), Some("Address could NOT be extracted from the document.")),
        EdnaScoreCardEntry(Some("dv:24"), Some("false")),
        EdnaScoreCardEntry(Some("dv:13"), Some("MARIUSZ BOHDAN FIKUS")),
        EdnaScoreCardEntry(Some("dv:9"), Some("false")),
        EdnaScoreCardEntry(Some("dv:12"), Some("ACUANT")),
        EdnaScoreCardEntry(Some("dv:8"), Some("false")),
        EdnaScoreCardEntry(Some("dv:11"), Some("false"))
      )
    )
  )

  val attributesResponse = AttributesResponse(
    progress = "NEW"
  )
}
