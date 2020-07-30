import {
  SESSION_ID,
  SESSION_STATE,
  LOGO,
  ORGANISATION_NAME,
  USER_ROLE
} from '../helpers/constants';

const {
  REACT_APP_VERIFIER,
  REACT_APP_ISSUER,
  REACT_APP_GRPC_CLIENT,
  REACT_APP_WALLET_GRPC_CLIENT
} = window._env_;

const issuerId = getFromLocalStorage('issuerId') || REACT_APP_ISSUER;
const verifierId = getFromLocalStorage('verifierId') || REACT_APP_VERIFIER;

function getUserId(isIssuer) {
  return isIssuer ? issuerId : verifierId;
}

export const config = {
  issuerId,
  verifierId,
  sessionId: newSession(SESSION_ID),
  sessionState: newSession(SESSION_STATE),
  grpcClient: getFromLocalStorage('backendUrl') || REACT_APP_GRPC_CLIENT,
  walletGrpcClient: getFromLocalStorage('walletUrl') || REACT_APP_WALLET_GRPC_CLIENT,
  userId: getUserId,
  userRole: newConfig(USER_ROLE),
  organizationName: newConfig(ORGANISATION_NAME),
  logo: newConfig(LOGO)
};

function getFromLocalStorage(key) {
  return window.localStorage.getItem(key);
}

function setToLocalStorage(key) {
  return value => window.localStorage.setItem(key, value);
}

function removeFromLocalStorage(key) {
  return () => window.localStorage.removeItem(key);
}
function getFromSessionStorage(key) {
  return window.sessionStorage.getItem(key);
}

function setToSessionStorage(key) {
  return value => window.sessionStorage.setItem(key, value);
}

function removeFromSessionStorage(key) {
  return () => window.sessionStorage.removeItem(key);
}

function newConfig(key) {
  return {
    get: () => getFromLocalStorage(key),
    set: setToLocalStorage(key),
    remove: removeFromLocalStorage(key)
  };
}

function newSession(key) {
  return {
    get: () => getFromSessionStorage(key),
    set: setToSessionStorage(key),
    remove: removeFromSessionStorage(key)
  };
}
