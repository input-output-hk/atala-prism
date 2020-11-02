package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.SHA256Digest

trait Credential[+C] {
  def contentBytes: IndexedSeq[Byte]
  def content: C
}

trait VerifiableCredential[+C, S, P, U] extends Credential[C] {
  def canonicalForm: String

  def hash: SHA256Digest
  def signature: Option[S]

  def isSigned: Boolean = signature.isDefined
  def isUnverifiable: Boolean = signature.isEmpty

  def sign(signature: IndexedSeq[Byte] => S): VerifiableCredential[C, S, P, U]
  def isValidSignature(verify: (IndexedSeq[Byte], S) => Boolean): Boolean
}
