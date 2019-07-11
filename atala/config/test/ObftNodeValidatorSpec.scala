package atala.config
package test

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

class ObftNodeValidatorSpec extends WordSpec with MustMatchers {

  val validator = ObftNodeValidator()

  "A ObftNodeValidator" should {

    "use the underlaying validators" in {
      def hasErrors(thisIndex: Int, otherIndexes: Int*): Boolean = {
        val candidate = node(thisIndex, otherIndexes: _*)
        validator.validateObftNode(candidate).isDefined
      }

      hasErrors(1, 2, 3, 4) must be(false)
      hasErrors(3, 1, 2, 4) must be(false)
      hasErrors(4, 1, 2, 3) must be(false)
      hasErrors(1, 2, 3) must be(false)
      hasErrors(1) must be(false)

      hasErrors(0, 2, 3, 4) must be(true)
      hasErrors(1, 1, 2, 3) must be(true)
      hasErrors(5, 1, 2, 3) must be(true)
      hasErrors(1, 2, 3, 5) must be(true)
      hasErrors(2) must be(true)
    }

  }

  def node(thisIndex: Int, otherIndexes: Int*): ObftNode =
    ObftNode(
      serverIndex = thisIndex,
      publicKey = null,
      privateKey = null,
      database = null,
      remoteNodes = otherIndexes.toSet.map(remote),
      timeSlotDuration = null,
      address = null
    )

  def remote(index: Int): RemoteObftNode =
    RemoteObftNode(
      serverIndex = index,
      publicKey = null,
      address = null
    )

}
