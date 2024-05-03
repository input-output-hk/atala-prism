package io.iohk.atala.prism.node.models

case class TimestampInfo(
    atalaBlockTimestamp: Long,
    atalaBlockSequenceNumber: Int,
    operationSequenceNumber: Int
)
