package io.iohk.indy

import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger._
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet

import scala.util.Try

object IssueCredential extends HasMain {
  def main(args: Array[String]): Unit = {
    val poolName = DEFAULT_POOL_NAME
    val stewardSeed = STEWARD_SEED
    val walletConfig = DEFAULT_WALLET_CONFIG_JSON
    val walletCredentials = DEFAULT_WALLET_CREDENTIALS_JSON
    prepareCleanEnvironment()

    val proverDID = "VsKV7grR1BUE29mG2Fm2kX"
    val proverWalletName = "prover_wallet"
    val proverWalletConfig = walletConfigJson(proverWalletName)
    val proverWalletCredentials = DEFAULT_WALLET_CREDENTIALS_JSON
    Try {
      Wallet.deleteWallet(proverWalletConfig, proverWalletCredentials)
    }

    // Step 2 code goes here.
    println("\n1. Creating a new local pool ledger configuration that can be used later to connect pool nodes.\n")
    Pool.createPoolLedgerConfig(poolName, DEFAULT_POOL_CONFIG_JSON).get

    println("\n2. Open pool ledger and get the pool handle from libindy.\n")
    val pool = Pool.openPoolLedger(poolName, "{}").get

    println("\n3. Creates a new secure wallet\n")
    Wallet.createWallet(walletConfig, walletCredentials).get

    println("\n4. Open wallet and get the wallet handle from libindy\n")
    val walletHandle = Wallet.openWallet(walletConfig, walletCredentials).get

    println("\n5. Generating and storing steward DID and Verkey\n")
    val did_json = s"""{"seed": "$stewardSeed"}"""
    val stewardResult = Did.createAndStoreMyDid(walletHandle, did_json).get
    val defaultStewardDid = stewardResult.getDid
    println("Steward DID: " + defaultStewardDid)
    println("Steward Verkey: " + stewardResult.getVerkey)

    println("\n6. Generating and storing Trust Anchor DID and Verkey\n")
    val trustAnchorResult = Did.createAndStoreMyDid(walletHandle, "{}").get
    val trustAnchorDID = trustAnchorResult.getDid
    val trustAnchorVerkey = trustAnchorResult.getVerkey
    println("Trust anchor DID: " + trustAnchorDID)
    println("Trust anchor Verkey: " + trustAnchorVerkey)

    println("\n7. Build NYM request to add Trust Anchor to the ledger\n")
    val nymRequest = buildNymRequest(defaultStewardDid, trustAnchorDID, trustAnchorVerkey, null, "TRUST_ANCHOR").get
    println("NYM request JSON:\n" + nymRequest)

    println("\n8. Sending the nym request to ledger\n")
    val nymResponseJson = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, nymRequest).get
    println("NYM transaction response:\n" + nymResponseJson)

    println("\n9. Build the SCHEMA request to add new schema to the ledger as a Steward\n")
    val name = "gvt"
    val version = "1.0"
    val attributes =
      """
        |[
        |  "age",
        |  "sex",
        |  "height",
        |  "name"
        |]
        |""".stripMargin

    val createSchemaResult = Anoncreds.issuerCreateSchema(trustAnchorDID, name, version, attributes).get
    val schemaDataJSON = createSchemaResult.getSchemaJson
    println("Schema: " + schemaDataJSON)

    println("\n11. Creating and storing CRED DEF using anoncreds as Trust Anchor, for the given Schema\n")
    val defaultCredentialDefinitionConfig = """{"support_revocation":false}"""
    val issuer1CreateGvtCredDefResult = Anoncreds
      .issuerCreateAndStoreCredentialDef(
        walletHandle,
        trustAnchorDID,
        schemaDataJSON,
        "tag",
        null,
        defaultCredentialDefinitionConfig
      )
      .get
    val credDef = issuer1CreateGvtCredDefResult.getCredDefJson
    val credDefId = issuer1CreateGvtCredDefResult.getCredDefId
    println("Returned Cred Definition:\n" + credDef)

    // Step 3 code goes here.
    println("\n12. Creating Prover wallet and opening it to get the handle\n")
    Wallet.createWallet(proverWalletConfig, proverWalletCredentials).get()
    val proverWalletHandle = Wallet.openWallet(proverWalletConfig, proverWalletCredentials).get

    println("\n13. Prover is creating Link Secret\n")
    val masterSecretName = "link_secret"
    Anoncreds.proverCreateMasterSecret(proverWalletHandle, masterSecretName).get

    // Step 4 code goes here.
    println("\n14. Issuer (Trust Anchor) is creating a Credential Offer for Prover\n")
    val credOfferJSON = Anoncreds.issuerCreateCredentialOffer(walletHandle, credDefId).get
    println("Claim Offer:\n" + credOfferJSON)

    println("\n15. Prover creates Credential Request\n")
    val credRequest = Anoncreds
      .proverCreateCredentialReq(proverWalletHandle, proverDID, credOfferJSON, credDef, masterSecretName)
      //    .proverCreateAndStoreClaimReq(proverWalletHandle, proverDID, credOfferJSON, claimDef, masterSecretName)
      .get
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
    val createClaimResult = Anoncreds
      .issuerCreateCredential(
        walletHandle,
        credOfferJSON,
        credRequest.getCredentialRequestJson,
        credAttribsJson,
        null,
        -1
      )
      .get
    val credJSON = createClaimResult.getCredentialJson
    println("Claim:\n" + credJSON)

    println("\n17. Prover processes and stores credential\n")
    Anoncreds
      .proverStoreCredential(
        proverWalletHandle,
        null,
        credRequest.getCredentialRequestMetadataJson,
        credJSON,
        credDef,
        null
      )
      .get

    // We have issued, received, and stored a credential. Mission accomplished!

    // Now do some cleanup.
    println("\n18. Close and delete wallet\n")
    proverWalletHandle.closeWallet.get
    Wallet.deleteWallet(proverWalletConfig, proverWalletCredentials).get

    println("\n18. Close and delete wallet\n")
    walletHandle.closeWallet.get
    Wallet.deleteWallet(walletConfig, walletCredentials).get

    println("\n19. Close pool\n")
    pool.closePoolLedger.get

    println("\n20. Delete pool ledger config\n")
    Pool.deletePoolLedgerConfig(poolName).get
  }
}
