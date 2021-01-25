import { v4 as uuidv4 } from 'uuid';
import { fromByteArray } from 'base64-js';
import { retry } from '../../helpers/promises';

import {
  UNLOCKED,
  LOCKED,
  BROWSER_WALLET_CHECK_INTERVAL_MS,
  BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS,
  MISSING_WALLET_ERROR
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
      .catch(() => {
        reject(MISSING_WALLET_ERROR);
      });
  });
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
    if (isSessionError(error)) this.handleSessionError();
    throw error;
  }
}

async function signCredentials(unsignedCredentials) {
  const { sessionId } = this.session;
  const signRequests = unsignedCredentials.map(unsignedCredential => {
    const payload = {
      id: unsignedCredential.credentialid,
      properties: JSON.parse(unsignedCredential.credentialdata)
    };

    // FIXME: remove the courses property from the transcript credential
    // as the wallet doesn't support arrays as properties
    delete payload.properties.courses;

    return window.prism.requestSignature(sessionId, JSON.stringify(payload));
  });
  return Promise.all(signRequests);
}

function Wallet(config) {
  this.config = config;
  this.session = defaultSessionState;
  this.handleSessionError = () => {};
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
Wallet.prototype.verifyRegistration = verifyRegistration;
Wallet.prototype.setSessionState = setSessionState;
Wallet.prototype.setSessionErrorHandler = setSessionErrorHandler;
Wallet.prototype.getNonce = getNonce;
Wallet.prototype.signMessage = signMessage;
Wallet.prototype.signCredentials = signCredentials;

export default Wallet;
