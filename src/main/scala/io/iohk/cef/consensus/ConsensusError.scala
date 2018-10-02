package io.iohk.cef.consensus
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.BlockHeader

sealed trait ConsensusError extends ApplicationError

case class UnableToProcessBlock[Header <: BlockHeader](header: Header) extends ConsensusError
