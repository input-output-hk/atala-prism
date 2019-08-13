package io.iohk.indy

import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateAndStoreCredentialDef
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger._
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet

object SaveSchemaAndCredentialDefinition extends HasMain {
  def main(args: Array[String]): Unit = {
    val poolName = DEFAULT_POOL_NAME
    val stewardSeed = STEWARD_SEED
    val walletConfig = DEFAULT_WALLET_CONFIG_JSON
    val walletCredentials = DEFAULT_WALLET_CREDENTIALS_JSON
    prepareCleanEnvironment()

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

    // Step 3 code goes here.
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
    // this is on the example but doesn't seem to be required, I'm leaving it just in case
//    val schemaRequest = buildSchemaRequest(defaultStewardDid, schemaDataJSON).get
//    println("Schema request:\n" + schemaRequest)
//
//    println("\n10. Sending the SCHEMA request to the ledger\n")
//    val schemaResponse = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, schemaRequest).get
//    println("Schema response:\n" + schemaResponse)

    // Step 4 code goes here.
    println("\n11. Creating and storing CRED DEF using anoncreds as Trust Anchor, for the given Schema\n")

    val defaultCredentialDefinitionConfig = """{"support_revocation":false}"""
    val issuer1CreateGvtCredDefResult = issuerCreateAndStoreCredentialDef(
      walletHandle,
      trustAnchorDID,
      schemaDataJSON,
      "tag",
      null,
      defaultCredentialDefinitionConfig
    ).get
    val credDef = issuer1CreateGvtCredDefResult.getCredDefJson
    println("Returned Cred Definition:\n" + credDef)

    // Some cleanup code.
    println("\n12. Close and delete wallet\n")
    walletHandle.closeWallet.get
    Wallet.deleteWallet(walletConfig, walletCredentials).get

    println("\n13. Close pool\n")
    pool.closePoolLedger.get

    println("\n14. Delete pool ledger config\n")
    Pool.deletePoolLedgerConfig(poolName).get
  }
}
