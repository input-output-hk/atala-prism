package io.iohk.indy

import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger._
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet
import org.json.JSONObject

object RotateKey extends HasMain {
  def main(args: Array[String]): Unit = {
    val poolName = DEFAULT_POOL_NAME
    val stewardSeed = STEWARD_SEED
    val walletConfig = DEFAULT_WALLET_CONFIG_JSON
    val walletCredentials = DEFAULT_WALLET_CREDENTIALS_JSON
    prepareCleanEnvironment()

    // Step 2 code goes here.
    // Tell SDK which pool you are going to use. You should have already started
    // this pool using docker compose or similar. Here, we are dumping the config
    // just for demonstration purposes.
    println("\n1. Creating a new local pool ledger configuration that can be used later to connect pool nodes.\n")
    Pool.createPoolLedgerConfig(poolName, DEFAULT_POOL_CONFIG_JSON).get

    println("\n2. Open pool ledger and get the pool handle from libindy.\n")
    val pool = Pool.openPoolLedger(poolName, "{}").get

    println("\n3. Creates a new secure wallet\n")
    Wallet.createWallet(walletConfig, walletCredentials).get

    println("\n4. Open wallet and get the wallet handle from libindy\n")
    val walletHandle = Wallet.openWallet(walletConfig, walletCredentials).get

    // First, put a steward DID and its keypair in the wallet. This doesn't write anything to the ledger,
    // but it gives us a key that we can use to sign a ledger transaction that we're going to submit later.
    println("\n5. Generating and storing steward DID and Verkey\n")

    // The DID and public verkey for this steward key are already in the ledger; they were part of the genesis
    // transactions we told the SDK to start with in the previous step. But we have to also put the DID, verkey,
    // and private signing key into our wallet, so we can use the signing key to submit an acceptably signed
    // transaction to the ledger, creating our *next* DID (which is truly new). This is why we use a hard-coded seed
    // when creating this DID--it guarantees that the same DID and key material are created that the genesis txns
    // expect.
    val did_json = s"""{ "seed": "$stewardSeed"}"""
    val stewardResult = Did.createAndStoreMyDid(walletHandle, did_json).get
    val defaultStewardDid = stewardResult.getDid
    println("Steward did: " + defaultStewardDid)

    // Now, create a new DID and verkey for a trust anchor, and store it in our wallet as well. Don't use a seed;
    // this DID and its keyas are secure and random. Again, we're not writing to the ledger yet.
    println("\n6. Generating and storing Trust Anchor DID and Verkey\n")
    val trustAnchorResult = Did.createAndStoreMyDid(walletHandle, "{}").get
    val trustAnchorDID = trustAnchorResult.getDid
    val trustAnchorVerkey = trustAnchorResult.getVerkey
    println("Trust anchor DID: " + trustAnchorDID)
    println("Trust anchor Verkey: " + trustAnchorVerkey)

    // Here, we are building the transaction payload that we'll send to write the Trust Anchor identity to the ledger.
    // We submit this transaction under the authority of the steward DID that the ledger already recognizes.
    // This call will look up the private key of the steward DID in our wallet, and use it to sign the transaction.
    println("\n7. Build NYM request to add Trust Anchor to the ledger\n")
    val nymRequest = buildNymRequest(defaultStewardDid, trustAnchorDID, trustAnchorVerkey, null, "TRUST_ANCHOR").get
    println("NYM request JSON:\n" + nymRequest)

    // Now that we have the transaction ready, send it. The building and the sending are separate steps because some
    // clients may want to prepare transactions in one piece of code (e.g., that has access to privileged backend systems),
    // and communicate with the ledger in a different piece of code (e.g., that lives outside the safe internal
    // network).
    println("\n8. Sending NYM request to ledger\n")
    val nymResponseJson = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, nymRequest).get
    println("NYM transaction response:\n" + nymResponseJson)

    // At this point, we have successfully written a new identity to the ledger.
    // Step 3 code goes here.
    println("\n9. Generating new Verkey of Trust Anchor in the wallet\n")
    val newTrustAnchorVerkey = Did.replaceKeysStart(walletHandle, trustAnchorDID, "{}").get
    println("New Trust Anchor's Verkey: " + newTrustAnchorVerkey)

    println("\n10. Building NYM request to update new verkey to ledger\n")
    val nymUpdateRequest =
      buildNymRequest(trustAnchorDID, trustAnchorDID, newTrustAnchorVerkey, null, "TRUST_ANCHOR").get
    println("NYM request:\n" + nymUpdateRequest)

    println("\n11. Sending NYM request to the ledger\n")
    val nymUpdateResponse = signAndSubmitRequest(pool, walletHandle, trustAnchorDID, nymUpdateRequest).get
    println("NYM response:\n" + nymUpdateResponse)

    println("\n12. Applying new Trust Anchor's Verkey in wallet\n")
    Did.replaceKeysApply(walletHandle, trustAnchorDID)

    // Step 4 code goes here.
    println("\n13. Reading new Verkey from wallet\n")
    val trustAnchorVerkeyFromWallet = Did.keyForLocalDid(walletHandle, trustAnchorDID).get

    println("\n14. Building GET_NYM request to get Trust Anchor from Verkey\n")
    val getNymRequest = buildGetNymRequest(trustAnchorDID, trustAnchorDID).get
    println("GET_NYM request:\n" + getNymRequest)

    println("\n15. Sending GET_NYM request to ledger\n")
    val getNymResponse = submitRequest(pool, getNymRequest).get
    println("GET_NYM response:\n" + getNymResponse)

    println("\n16. Comparing Trust Anchor verkeys\n")
    println("Written by Steward: " + trustAnchorDID)
    println("Current from wallet: " + trustAnchorVerkeyFromWallet)
    val responseData = new JSONObject(getNymResponse).getJSONObject("result").getString("data")
    val trustAnchorVerkeyFromLedger = new JSONObject(responseData).getString("verkey")
    println("Current from ledger: " + trustAnchorVerkeyFromLedger)
    val `match` = !trustAnchorDID.equals(trustAnchorVerkeyFromWallet) && trustAnchorVerkeyFromWallet == trustAnchorVerkeyFromLedger
    println("Matching: " + `match`)

    // Do some cleanup.
    println("\n17. Close and delete wallet\n")
    walletHandle.closeWallet.get
    Wallet.deleteWallet(walletConfig, walletCredentials).get

    println("\n18. Close pool\n")
    pool.closePoolLedger.get

    println("\n19. Delete pool ledger config\n")
    Pool.deletePoolLedgerConfig(poolName).get
  }
}
