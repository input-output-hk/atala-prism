package io.iohk.dids

sealed trait DIDException extends Exception
class UnknownMethodException(method: String) extends Exception(s"Unknown DID method: $method") with DIDException
class MalformedDIDException(message: String) extends Exception(message) with DIDException
class UpdateException(message: String) extends Exception(message) with DIDException
