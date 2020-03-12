import { LOGO, ORGANISATION_NAME, USER_ROLE } from '../helpers/constants';

const {
  REACT_APP_VERIFIER,
  REACT_APP_ISSUER,
  REACT_APP_GRPC_CLIENT,
  REACT_APP_WALLET_GRPC_CLIENT
} = window._env_;

const issuerId = get('issuerId') || REACT_APP_ISSUER;
const verifierId = get('verifierId') || REACT_APP_VERIFIER;

function getUserId(isIssuer) {
  return isIssuer ? issuerId : verifierId;
}

export const config = {
  issuerId,
  verifierId,
  grpcClient: get('backendUrl') || REACT_APP_GRPC_CLIENT,
  walletGrpcClient: get('walletUrl') || REACT_APP_WALLET_GRPC_CLIENT,
  userId: getUserId,
  userRole: newConfig(USER_ROLE),
  organizationName: newConfig(ORGANISATION_NAME),
  logo: newConfig(LOGO)
};

function get(key) {
  return window.localStorage.getItem(key);
}

function set(key) {
  return value => window.localStorage.setItem(key, value);
}

function remove(key) {
  return () => window.localStorage.removeItem(key);
}

function newConfig(key) {
  return {
    get: () => get(key),
    set: set(key),
    remove: remove(key)
  };
}
