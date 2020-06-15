package io.iohk.node.poc.jwsflow

import java.net.URI
import java.time.LocalDate

import io.circe.Json
import net.jtownson.odyssey.VC
import net.jtownson.odyssey.VC.VCField.{CredentialSubjectField, EmptyField, IssuanceDateField, IssuerField}
import org.scalatest.OptionValues._

// This SDK would allow to build generic credentials and manipulate them
// For this toy example, the credential model is a String that represents a JSON
// and we didn't add nice builders, we just take fixed values for illustration
// to build a degree credential
object GenericCredentialsSDKWithOdyssey {

  def buildGenericCredential(
      issuerDID: String,
      claims: Json
  ): VC[EmptyField with IssuerField with IssuanceDateField with CredentialSubjectField] = {

    val didURI = new URI(issuerDID)
    // TODO: It would be nice to be able to give SubgjetAttributes as a JSON directly
    val claimsSeq = claims.asObject.value.toList

    VC()
      .withIssuer(didURI)
      .withIssuanceDate(LocalDate.now().atStartOfDay())
      .withSubjectAttributes(claimsSeq: _*)
  }
}
