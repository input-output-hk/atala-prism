import { v4 as uuidv4 } from 'uuid';
import { fromByteArray } from 'base64-js';
import { retry } from '../../helpers/promises';
import { getCredentialTypeAttributes } from '../helpers/credentialTypeHelpers';
import {
  UNLOCKED,
  LOCKED,
  BROWSER_WALLET_CHECK_INTERVAL_MS,
  BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS,
  MISSING_WALLET_ERROR,
  CREDENTIAL_VERIFICATION_ERRORS
} from '../../helpers/constants';

const RETRIES = 100;

function getSessionFromExtension({ timeout = BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } = {}) {
  return Promise.race([this.repeatGetSessionRequest(), timeoutPromise(timeout)]);
}

function repeatGetSessionRequest() {
  return retry(() => this.getSessionRequest(), RETRIES, BROWSER_WALLET_CHECK_INTERVAL_MS);
}

const timeoutPromise = ms =>
  new Promise(resolve => {
    setTimeout(resolve, ms, { error: MISSING_WALLET_ERROR });
  });

function getSessionRequest() {
  return new Promise((resolve, reject) => {
    const extensionApi = window.prism;

    if (!extensionApi) reject();

    extensionApi
      .login()
      .then(session => {
        if (isValidSession(session)) {
          const newSessionState = this.setSessionState(session);
          resolve({ sessionData: newSessionState, error: null });
        } else resolve({ error: MISSING_WALLET_ERROR });
      })
      .catch(() => {
        resolve({ error: MISSING_WALLET_ERROR });
      });
  });
}

function isValidSession(session) {
  // TODO: improve token validation?
  return !!session.sessionId;
}

function setSessionState(sessionData) {
  this.session = sessionData?.sessionId
    ? {
        sessionId: sessionData?.sessionId,
        sessionState: UNLOCKED,
        organisationName: sessionData.name,
        logo: fromByteArray(sessionData.logo)
      }
    : defaultSessionState;

  return this.session;
}

function setSessionErrorHandler(sessionErrorHandler) {
  this.handleSessionError = sessionErrorHandler;
}

function getNonce() {
  const buffer = [];
  uuidv4(null, buffer);
  return Uint8Array.from(buffer);
}

function isSessionError(error) {
  const LOCKED_WALLET_ERROR = 'You need to create the wallet before logging in';
  return (
    error.message.includes(LOCKED_WALLET_ERROR) || error.message.includes(MISSING_WALLET_ERROR)
  );
}

async function signMessage(unsignedRequest, timeout) {
  try {
    const { sessionId } = this.session;
    const requestBytes = unsignedRequest.serializeBinary();
    const result = timeout
      ? await Promise.race([
          window.prism.signConnectorRequest(sessionId, requestBytes),
          timeoutPromise(timeout)
        ])
      : await window.prism.signConnectorRequest(sessionId, requestBytes);

    if (result.error) throw new Error(result.error);
    return result;
  } catch (error) {
    if (!isSessionError(error)) throw error;
    this.handleSessionError();
    return { sessionError: true };
  }
}

async function signCredentials(unsignedCredentials) {
  const { sessionId } = this.session;
  const credentialTypeAttributes = await getCredentialTypeAttributes(unsignedCredentials);

  const signRequests = unsignedCredentials.map(({ credentialId, credentialData: { html } }) => {
    const payload = {
      id: credentialId,
      properties: { html, ...credentialTypeAttributes }
    };

    return window.prism.requestSignature(sessionId, JSON.stringify(payload));
  });
  return Promise.all(signRequests);
}

async function verifyCredential(signedCredential, inclusionProof) {
  const { sessionId } = this.session;
  // This method checks that:
  // - the signature is valid
  // - it was signed with a valid key (at the time of signing)
  // - neither the batch nor the credential has been revoked
  // - the hash has been included in the transaction
  const verificationErrors = await window.prism.verifySignedCredential(
    sessionId,
    signedCredential,
    inclusionProof
  );
  return getVerificationResults(verificationErrors);
}

function getVerificationResults(errors) {
  return Object.keys(CREDENTIAL_VERIFICATION_ERRORS).reduce(
    (acc, key) =>
      Object.assign(acc, {
        [key]: errors.some(error => error.includes(CREDENTIAL_VERIFICATION_ERRORS[key]))
      }),
    {}
  );
}

function revokeCredentials(credentials) {
  const { sessionId } = this.session;

  const revokeRequests = credentials.map(cred =>
    window.prism.revokeCredential(
      sessionId,
      cred.encodedSignedCredential,
      cred.batchId,
      cred.issuanceOperationHash,
      cred.credentialId
    )
  );
  return Promise.all(revokeRequests);
}

function Wallet(config) {
  this.config = config;
  this.session = defaultSessionState;
  this.handleSessionError = () => {
    // This function should be overridden using setSessionErrorHandler
  };
}

const defaultSessionState = {
  sessionId: null,
  sessionState: LOCKED,
  organisationname: null,
  logo: null
};

Wallet.prototype.getSessionFromExtension = getSessionFromExtension;
Wallet.prototype.repeatGetSessionRequest = repeatGetSessionRequest;
Wallet.prototype.getSessionRequest = getSessionRequest;
Wallet.prototype.setSessionState = setSessionState;
Wallet.prototype.setSessionErrorHandler = setSessionErrorHandler;
Wallet.prototype.getNonce = getNonce;
Wallet.prototype.signMessage = signMessage;
Wallet.prototype.signCredentials = signCredentials;
Wallet.prototype.verifyCredential = verifyCredential;
Wallet.prototype.revokeCredentials = revokeCredentials;

export default Wallet;
