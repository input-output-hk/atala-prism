package io.iohk.cvp.intdemo

import java.time.LocalDate

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.EitherValues._

class IdDataSpec extends FlatSpec {

  "IDData" should "extract the data from an IDCredential" in {
    val expectedFirstName = "first-name"
    val expectedDoB = LocalDate.of(1973, 6, 6)
    val credential = IdServiceImpl.getIdCredential((expectedFirstName, expectedDoB))
    val idData = IdData.toIdData(credential).right.value

    idData.name shouldBe expectedFirstName
    idData.dob shouldBe expectedDoB
  }
}
