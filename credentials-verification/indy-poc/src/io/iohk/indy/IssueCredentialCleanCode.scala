package io.iohk.indy

import io.iohk.indy.helpers.{IndyAgent, IndyLedger, IndyPool}
import io.iohk.indy.models._
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.pool.Pool

object IssueCredentialCleanCode extends HasMain {

  def attributesJson(names: String*): String = {
    names.map(s => s""""$s"""").mkString("[", ",", "]")
  }

  def main(args: Array[String]): Unit = {
    val poolName = DEFAULT_POOL_NAME
    prepareCleanEnvironment()

    val pool = IndyPool(DEFAULT_POOL_NAME, GENESIS_TXN)
    val stewardAgent = IndyAgent(walletName = DEFAULT_WALLET_NAME, walletKey = DEFAULT_WALLET_KEY)
    val stewardDid = stewardAgent.createDid(STEWARD_SEED)
    val trustAnchorDid = stewardAgent.newDid()
    val nymRequest = IndyLedger.buildNymRequest(stewardDid.did, trustAnchorDid, UserRole.TrustAnchor)
    val nymSignedRequest = stewardAgent.signRequest(stewardDid.did, nymRequest)
    val nymResponseJson = pool.submitRequest(nymSignedRequest.string)
    println("NYM transaction response:\n" + nymResponseJson.string)

    println("\n9. Build the SCHEMA request to add new schema to the ledger as a Steward\n")
    val name = "gvt"
    val version = "1.0"

    val attributes = attributesJson("age", "sex", "height", "name")
    val createSchemaResult = Anoncreds.issuerCreateSchema(trustAnchorDid.did.string, name, version, attributes).get
    println("Schema: " + createSchemaResult.getSchemaJson)

    println("\n11. Creating and storing CRED DEF using anoncreds as Trust Anchor, for the given Schema\n")
    val issuer1CredentialDef =
      stewardAgent.issuerCreateCredentialDefinition(trustAnchorDid.did, JsonString(createSchemaResult.getSchemaJson))

    println("Returned Cred Definition:\n" + issuer1CredentialDef.json)

    // Step 3 code goes here.
    println("\n12. Creating Prover wallet and opening it to get the handle\n")
    val proverDid = Did("VsKV7grR1BUE29mG2Fm2kX")
    val proverAgent = IndyAgent(walletName = "prover-wallet", walletKey = DEFAULT_WALLET_KEY)

    println("\n13. Prover is creating Link Secret\n")
    val masterSecretName = MasterSecretId("link_secret")
    proverAgent.proverCreateMasterSecret(masterSecretName)

    // Step 4 code goes here.
    println("\n14. Issuer (Trust Anchor) is creating a Credential Offer for Prover\n")
    val credOfferJSON = stewardAgent.issuerCreateCredentialOffer(CredentialDefinitionId(issuer1CredentialDef.id))
    println("Claim Offer:\n" + credOfferJSON)

    println("\n15. Prover creates Credential Request\n")
    val credRequest =
      proverAgent.proverCreateCredentialRequest(proverDid, credOfferJSON, issuer1CredentialDef, masterSecretName)
    println("Cred Request:\n" + credRequest)

    println("\n16. Issuer (Trust Anchor) creates Credential for Credential Request\n")
    // Encoded value of non-integer attribute is SHA256 converted to decimal
    // note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
    val credAttribsJson =
      """
      |{
      |  "sex": ["male", "5944657099558967239210949258394887428692050081607692519917050011144233115103"],
      |  "name": ["Alex", "99262857098057710338306967609588410025648622308394250666849665532448612202874"],
      |  "height": ["175", "175"],
      |  "age": ["28", "28"]
      |}
      |""".stripMargin
    val createCredentialResult = stewardAgent.issuerCreateCredential(
      offerJson = credOfferJSON,
      requestJson = credRequest.json,
      attributesJson = JsonString(credAttribsJson)
    )

    println("Claim:\n" + createCredentialResult.getCredentialJson)

    println("\n17. Prover processes and stores credential\n")
    proverAgent.proverStoreCredential(
      JsonString(createCredentialResult.getCredentialJson),
      credRequest,
      issuer1CredentialDef
    )
    // We have issued, received, and stored a credential. Mission accomplished!

    // Now do some cleanup.
    println("\n18. Close pool\n")
    pool.close()

    println("\n19. Delete pool ledger config\n")
    Pool.deletePoolLedgerConfig(poolName).get
  }
}
