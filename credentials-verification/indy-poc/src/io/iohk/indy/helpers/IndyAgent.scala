package io.iohk.indy.helpers

import io.iohk.indy.models._
import io.iohk.indy.{walletConfigJson, walletCredentialsJson}
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.did.{Did => IndyDid}
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.wallet.Wallet

import scala.util.Try

class IndyAgent(wallet: Wallet) {
  def createDid(seed: String): WalletDid = {
    val json = s"""{ "seed": "$seed"}"""
    val result = IndyDid.createAndStoreMyDid(wallet, json).get
    WalletDid(did = Did(result.getDid), verificationKey = result.getVerkey)
  }

  def newDid(): WalletDid = {
    val result = IndyDid.createAndStoreMyDid(wallet, "{}").get
    WalletDid(did = Did(result.getDid), verificationKey = result.getVerkey)
  }

  def signRequest(submitterDid: Did, requestJson: JsonString): JsonString = {
    val signedRequest = Ledger.signRequest(wallet, submitterDid.string, requestJson.string).get
    JsonString(signedRequest)
  }

  def issuerCreateCredentialDefinition(trustAnchorDid: Did, schemaDataJson: JsonString): CredentialDefinition = {
    val defaultCredentialDefinitionConfig = """{"support_revocation":false}"""

    val result = Anoncreds
      .issuerCreateAndStoreCredentialDef(
        wallet,
        trustAnchorDid.string,
        schemaDataJson.string,
        "tag",
        null,
        defaultCredentialDefinitionConfig
      )
      .get

    CredentialDefinition(id = result.getCredDefId, json = JsonString(result.getCredDefJson))
  }

  def issuerCreateCredentialOffer(credentialDefinitionId: CredentialDefinitionId): JsonString = {
    val credentialOffer = Anoncreds.issuerCreateCredentialOffer(wallet, credentialDefinitionId.string).get
    JsonString(credentialOffer)
  }

  def issuerCreateCredential(offerJson: JsonString, requestJson: JsonString, attributesJson: JsonString) = {
    Anoncreds
      .issuerCreateCredential(
        wallet,
        offerJson.string,
        requestJson.string,
        attributesJson.string,
        null,
        -1
      )
      .get
  }

  def proverCreateMasterSecret(masterSecret: MasterSecretId): MasterSecretId = {
    val result = Anoncreds.proverCreateMasterSecret(wallet, masterSecret.string).get
    MasterSecretId(result)
  }

  def proverCreateCredentialRequest(
      proverDid: Did,
      credentialOfferJson: JsonString,
      credentialDefinition: CredentialDefinition,
      masterSecret: MasterSecretId
  ): CreateCredentialRequest = {
    val result = Anoncreds
      .proverCreateCredentialReq(
        wallet,
        proverDid.string,
        credentialOfferJson.string,
        credentialDefinition.json.string,
        masterSecret.string
      )
      .get

    CreateCredentialRequest(
      json = JsonString(result.getCredentialRequestJson),
      metadataJson = JsonString(result.getCredentialRequestMetadataJson)
    )
  }

  def proverStoreCredential(
      credentialJson: JsonString,
      request: CreateCredentialRequest,
      credentialDefinition: CredentialDefinition
  ): CredentialId = {
    val storedCredId = Anoncreds
      .proverStoreCredential(
        wallet,
        null,
        request.metadataJson.string,
        credentialJson.string,
        credentialDefinition.json.string,
        null
      )
      .get
    CredentialId(storedCredId)
  }
}

object IndyAgent {
  def apply(walletName: String, walletKey: String): IndyAgent = {
    val walletConfig = walletConfigJson(walletName)
    val walletCredentials = walletCredentialsJson(walletKey)
    Try {
      Wallet.deleteWallet(walletConfig, walletCredentials).get()
    }
    Wallet.createWallet(walletConfig, walletCredentials).get
    val wallet = Wallet.openWallet(walletConfig, walletCredentials).get
    new IndyAgent(wallet)
  }
}
