package obft

sealed abstract class PublicKey(val identifier: String) {
  override def equals(that: Any): Boolean =
    that match {
      case pk: PublicKey => identifier == pk.identifier
      case _ => false
    }
  override def toString(): String =
    s"PublicKey($identifier)"
}

sealed abstract class PrivateKey(val identifier: String) {
  override def equals(that: Any): Boolean =
    that match {
      case pk: PrivateKey => identifier == pk.identifier
      case _ => false
    }
  override def toString(): String =
    s"PrivateKey($identifier)"
}

case class KeyPair private (identifier: String) {
  case object PublicKey extends obft.PublicKey(identifier)
  case object PrivateKey extends obft.PrivateKey(identifier)
}

object KeyPair {
  private val words: List[String] =
    List("foo", "bar", "baz", "qux", "quux", "corge", "grault", "garply", "waldo", "fred", "plugh", "xyzzy", "thud")

  private var keyPairs: Stream[KeyPair] = ({
    def loop(v: Int): Stream[Int] = v #:: loop(v + 1)
    loop(0)
  }).flatMap(i => words.map(w => s"$w$i").toStream).map(KeyPair.apply)

  def gen(): KeyPair = KeyPair.synchronized {
    val r = keyPairs.head
    keyPairs = keyPairs.tail
    r
  }
}

case class Signature[T] private (signedStuff: T, signingKeyIdentifier: String) {
  def isSignatureOf(candidate: T, publicKey: PublicKey): Boolean =
    signedStuff == candidate && publicKey.identifier == signingKeyIdentifier
}

object Signature {

  def sign[T](signedStuff: T, privateKey: PrivateKey): Signature[T] =
    Signature(signedStuff, privateKey.identifier)

}

case class Hash[+T] private (identifier: String)
object Hash {
  def apply[T](t: T): Hash[T] =
    new Hash(t.toString)
}
