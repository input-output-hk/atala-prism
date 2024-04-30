package io.iohk.atala.prism.node.identity

import io.iohk.atala.prism.protos.node_models.AtalaOperation

sealed abstract class PrismDidFormatException(msg: String, cause: Option[Throwable] = None)
    extends Exception(msg, cause.orNull)

case object UnrecognizedPrismDidException extends PrismDidFormatException("Provided DID is not a valid PRISM DID")

case object CanonicalSuffixMatchStateException
    extends PrismDidFormatException("Canonical suffix does not match the computed state")

case class InvalidAtalaOperationException(e: Exception)
    extends PrismDidFormatException("Provided bytes do not encode a valid Atala Operation", Some(e))

case class CreateDidExpectedAsInitialState(operation: AtalaOperation.Operation)
    extends PrismDidFormatException(
      s"Provided initial state of long form Prism DID is ${operation.value}, CreateDid Atala operation expected"
    )
