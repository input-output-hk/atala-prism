package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.SHA256Digest

abstract class Credential[+C, S, P, U] {
  def contentBytes: IndexedSeq[Byte]
  def content: C
  def signature: Option[S]
  def isSigned: Boolean = signature.isDefined
  def isUnverifiable: Boolean = signature.isEmpty

  def canonicalForm: String
  def hash: SHA256Digest
  def sign(signature: IndexedSeq[Byte] => S): Credential[C, S, P, U]
  def isValidSignature(verify: (IndexedSeq[Byte], S) => Boolean): Boolean
}
