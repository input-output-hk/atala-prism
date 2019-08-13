package io.iohk.indy

import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger._
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet
import org.json.JSONObject

/**
  * Example demonstrating how to add DID with the role of Trust Anchor as Steward.
  * Uses seed to obtain Steward's DID which already exists on the ledger.
  * Then it generates new DID/Verkey pair for Trust Anchor.
  * Using Steward's DID, NYM transaction request is built to add Trust Anchor's DID and Verkey
  * on the ledger with the role of Trust Anchor.
  * Once the NYM is successfully written on the ledger, it generates new DID/Verkey pair that represents
  * a client, which are used to create GET_NYM request to query the ledger and confirm Trust Anchor's Verkey.
  * For the sake of simplicity, a single wallet is used. In the real world scenario, three different wallets
  * would be used and DIDs would be exchanged using some channel of communication
  */
object WriteDIDAndQueryVerKey extends HasMain {

  def main(args: Array[String]): Unit = {
    val poolName = DEFAULT_POOL_NAME
    val stewardSeed = STEWARD_SEED
    val walletConfig = DEFAULT_WALLET_CONFIG_JSON
    val walletCredentials = DEFAULT_WALLET_CREDENTIALS_JSON
    prepareCleanEnvironment()

    // Step 2 code goes here.
    // Tell SDK which pool you are going to use. You should have already started
    // this pool using docker compose or similar.
    println("\n1. Creating a new local pool ledger configuration that can be used later to connect pool nodes.\n")
    Pool.createPoolLedgerConfig(poolName, DEFAULT_POOL_CONFIG_JSON).get

    println("\n2. Open pool ledger and get the pool handle from libindy.\n")
    val pool = Pool.openPoolLedger(poolName, "{}").get

    println("\n3. Creates a new identity wallet\n")
    Wallet.createWallet(walletConfig, walletCredentials).get

    println("\n4. Open identity wallet and get the wallet handle from libindy\n")
    val walletHandle = Wallet.openWallet(walletConfig, walletCredentials).get

    // Step 3 code goes here.
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
    println("Steward DID: " + defaultStewardDid)
    println("Steward Verkey: " + stewardResult.getVerkey)

    // Now, create a new DID and verkey for a trust anchor, and store it in our wallet as well. Don't use a seed;
    // this DID and its keys are secure and random. Again, we're not writing to the ledger yet.
    println("\n6. Generating and storing Trust Anchor DID and Verkey\n")
    val trustAnchorResult = Did.createAndStoreMyDid(walletHandle, "{}").get
    val trustAnchorDID = trustAnchorResult.getDid
    val trustAnchorVerkey = trustAnchorResult.getVerkey
    println("Trust anchor DID: " + trustAnchorDID)
    println("Trust anchor Verkey: " + trustAnchorVerkey)

    // Step 4 code goes here.
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
    println("\n8. Sending the nym request to ledger\n")
    val nymResponseJson = signAndSubmitRequest(pool, walletHandle, defaultStewardDid, nymRequest).get
    println("NYM transaction response:\n" + nymResponseJson)

    // At this point, we have successfully written a new identity to the ledger. Our next step will be to query it.

    // Step 5 code goes here.
    // Here we are creating a third DID. This one is never written to the ledger, but we do have to have it in the
    // wallet, because every request to the ledger has to be signed by some requester. By creating a DID here, we
    // are forcing the wallet to allocate a keypair and identity that we can use to sign the request that's going
    // to read the trust anchor's info from the ledger.
    println("\n9. Generating and storing DID and Verkey to query the ledger with\n")
    val clientResult = Did.createAndStoreMyDid(walletHandle, "{}").get
    val clientDID = clientResult.getDid
    val clientVerkey = clientResult.getVerkey
    println("Client DID: " + clientDID)
    println("Client Verkey: " + clientVerkey)

    println("\n10. Building the GET_NYM request to query Trust Anchor's Verkey as the Client\n")
    val getNymRequest = buildGetNymRequest(clientDID, trustAnchorDID).get
    println("GET_NYM request json:\n" + getNymRequest)

    println("\n11. Sending the GET_NYM request to the ledger\n")
    val getNymResponse = submitRequest(pool, getNymRequest).get
    println("GET_NYM response json:\n" + getNymResponse)

    // See whether we received the same info that we wrote the ledger in step 4.
    println("\n12. Comparing Trust Anchor Verkey as written by Steward and as retrieved in Client's query\n")
    val responseData = new JSONObject(getNymResponse).getJSONObject("result").getString("data")
    val trustAnchorVerkeyFromLedger = new JSONObject(responseData).getString("verkey")
    println("Written by Steward: " + trustAnchorVerkey)
    println("Queried from Ledger: " + trustAnchorVerkeyFromLedger)
    println("Matching: " + trustAnchorVerkey.equals(trustAnchorVerkeyFromLedger))

    // Do some cleanup.
    println("\n13. Close and delete wallet\n")
    walletHandle.closeWallet.get
    Wallet.deleteWallet(walletConfig, walletCredentials).get

    println("\n14. Close pool\n")
    pool.closePoolLedger.get

    println("\n15. Delete pool ledger config\n")
    Pool.deletePoolLedgerConfig(poolName).get
  }
}
