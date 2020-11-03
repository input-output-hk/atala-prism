import { v4 as uuidv4 } from 'uuid';
import { fromByteArray } from 'base64-js';
import { retry } from '../../helpers/promises';

import {
  UNLOCKED,
  LOCKED,
  BROWSER_WALLET_CHECK_INTERVAL_MS,
  BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS,
  VERIFIER,
  ISSUER,
  MISSING_WALLET_ERROR
} from '../../helpers/constants';

function getSessionFromExtension({ timeout = BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } = {}) {
  return Promise.race([this.repeatGetSessionRequest(), timeoutPromise(timeout)]);
}

function repeatGetSessionRequest() {
  // retry(promise, retries, interval)
  return retry(() => this.getSessionRequest(), 100, BROWSER_WALLET_CHECK_INTERVAL_MS);
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
      .catch(error => {
        resolve({ error: MISSING_WALLET_ERROR });
      });
  });
}

function isValidSession(session) {
  // TODO: improve token validation?
  return !!session.sessionId;
}

function verifyRegistration() {
  return new Promise((resolve, reject) => {
    const extensionApi = window.prism;

    if (!extensionApi) reject(MISSING_WALLET_ERROR);

    extensionApi
      .login()
      .then(session => {
        if (isValidSession(session)) {
          resolve();
        } else reject(MISSING_WALLET_ERROR);
      })
      .catch(error => {
        reject(MISSING_WALLET_ERROR);
      });
  });
}

function setSessionState(sessionData) {
  this.session = {
    sessionId: sessionData?.sessionId,
    sessionState: UNLOCKED,
    userRole: sessionData.role?.toUpperCase(),
    organisationName: sessionData.name,
    logo: fromByteArray(sessionData.logo)
  };

  return this.session;
}

function clearSession() {
  this.session = defaultSessionState;
  return this.session;
}

function isIssuer() {
  return this.session.userRole === ISSUER;
}

function isVerifier() {
  return this.session.userRole === VERIFIER;
}

function getNonce() {
  const buffer = [];
  uuidv4(null, buffer);
  return Uint8Array.from(buffer);
}

async function signMessage(unsignedRequest) {
  const { sessionId } = this.session;

  const requestBytes = unsignedRequest.serializeBinary();
  return window.prism.signConnectorRequest(sessionId, requestBytes);
}

async function signCredentials(unsignedCredentials) {
  const { sessionId } = this.session;
  const signRequests = unsignedCredentials.map(unsignedCredential => {
    const payload = {
      id: unsignedCredential.credentialid,
      properties: JSON.parse(unsignedCredential.credentialdata)
    };
    return window.prism.requestSignature(sessionId, JSON.stringify(payload));
  });
  return Promise.all(signRequests);
}

function Wallet(config) {
  this.config = config;
  this.session = defaultSessionState;
}

const defaultSessionState = {
  sessionId: null,
  sessionState: LOCKED,
  role: null,
  organisationname: null,
  logo: null
};

Wallet.prototype.getSessionFromExtension = getSessionFromExtension;
Wallet.prototype.repeatGetSessionRequest = repeatGetSessionRequest;
Wallet.prototype.getSessionRequest = getSessionRequest;
Wallet.prototype.verifyRegistration = verifyRegistration;
Wallet.prototype.setSessionState = setSessionState;
Wallet.prototype.clearSession = clearSession;
Wallet.prototype.isIssuer = isIssuer;
Wallet.prototype.isVerifier = isVerifier;
Wallet.prototype.getNonce = getNonce;
Wallet.prototype.signMessage = signMessage;
Wallet.prototype.signCredentials = signCredentials;

export default Wallet;
