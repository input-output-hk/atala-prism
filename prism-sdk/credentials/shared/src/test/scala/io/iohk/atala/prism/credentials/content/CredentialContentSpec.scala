package io.iohk.atala.prism.credentials.content

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials.content.CredentialContent._
import io.iohk.atala.prism.credentials.utils.Mustache

class CredentialContentSpec extends AnyWordSpec with Matchers with EitherValues {

  "CredentialContent" should {
    "return the value by field" in new Fixtures {
      val content = CredentialContent(
        "credentialType" -> Values(1, 2, 3, "test"),
        "issuerDid" -> "did",
        "issuanceKeyId" -> 123,
        "credentialSubject" ->
          Fields(
            "field" -> Values(1, 5, "content"),
            "nested" -> Fields("another" -> 10)
          )
      )

      content.getValue("nonexistent") mustBe a[Left[FieldNotFoundException, _]]

      content.getString("issuerDid") mustBe Right("did")
      content.getInt("issuanceKeyId") mustBe Right(123)
      content.getSeq("credentialType") mustBe Right(Seq(1, 2, 3, "test"))
      content.getSubFields("credentialSubject") mustBe Right(
        IndexedSeq(
          "field" -> Seq(1, 5, "content"),
          "nested" -> IndexedSeq("another" -> 10)
        )
      )
    }

    "allow to access nested fields" in new Fixtures {
      val content = CredentialContent(
        "credentialSubject" -> Fields(
          "fieldName" -> true,
          "nested" -> Fields(
            "key1" -> "value",
            "key2" -> 123
          )
        )
      )

      content.getBoolean("credentialSubject.fieldName") mustBe Right(true)
      content.getString("credentialSubject.nested.key1") mustBe Right("value")
      content.getInt("credentialSubject.nested.key2") mustBe Right(123)

      content.getValue("credentialSubject.nonexistent") mustBe a[Left[FieldNotFoundException, _]]
      content.getValue("nonexistent.nonexistent2.nonexistent3") mustBe a[Left[FieldNotFoundException, _]]
    }

    "render html template if provided" in new Fixtures {
      val content = CredentialContent(
        "variable" -> "Hello World",
        "html" -> "{{ variable }}!!!"
      )

      val context = (field: String) =>
        content.getValue(field) match {
          case Left(error) => None
          case Right(value) => Some(value.value.toString)
        }

      content.getString("html").flatMap(Mustache.render(_, context)) mustBe Right("Hello World!!!")
    }
  }

  trait Fixtures {
    val credentialContent = CredentialContent(
      "credentialType" -> Values(1, 2, 3, "test"),
      "issuerDid" -> "did",
      "issuanceKeyId" -> 123,
      "credentialSubject" -> Fields("field" -> 1)
    )
  }

}
