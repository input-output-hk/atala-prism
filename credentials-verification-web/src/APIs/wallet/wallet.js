/* eslint import/no-unresolved: 0 */ // --> OFF
import grpcWeb from 'grpc-web';
import { WalletServicePromiseClient } from '../../protos/wallet/wallet_grpc_web_pb';
import {
  CreateWalletRequest,
  GetDIDRequest,
  LockWalletRequest,
  UnlockWalletRequest,
  GetWalletStatusRequest
} from '../../protos/wallet/wallet_pb';
import Logger from '../../helpers/Logger';
import { USER_ROLE, ORGANISATION_NAME, ISSUER, VERIFIER } from '../../helpers/constants';

const { REACT_APP_WALLET_GRPC_CLIENT } = window._env_;
const walletServicePromiseClient = new WalletServicePromiseClient(
  REACT_APP_WALLET_GRPC_CLIENT,
  null,
  null
);

export const getDid = async () => {
  const didRequest = new GetDIDRequest();

  const call = await walletServicePromiseClient.getDID(didRequest, {});

  const { did } = call.toObject();

  return did;
};

export const createWallet = async passphrase => {
  const createWalletRequest = new CreateWalletRequest();
  createWalletRequest.setPassphrase(passphrase);
  const response = await walletServicePromiseClient.createWallet(createWalletRequest);

  return response.toObject();
};

export const getWalletStatus = async () => {
  const getWalletStatusRequest = new GetWalletStatusRequest();
  const walletStatus = await walletServicePromiseClient.getWalletStatus(
    getWalletStatusRequest,
    null
  );

  return walletStatus.getStatus();
};

const cleanUserData = () => {
  localStorage.removeItem(USER_ROLE);
  localStorage.removeItem(ORGANISATION_NAME);
};

export const lockWallet = async () => {
  const lockRequest = new LockWalletRequest();
  await walletServicePromiseClient.lockWallet(lockRequest);

  cleanUserData();
};

const UserRoles = {
  0: ISSUER,
  1: VERIFIER
};

const translateUserRole = role => UserRoles[role];

const setUserData = (role, organisationName) => {
  const roleAsString = translateUserRole(role);
  localStorage.setItem(USER_ROLE, roleAsString);
  localStorage.setItem(ORGANISATION_NAME, organisationName);
};

export const unlockWallet = async passphrase => {
  const unlockRequest = new UnlockWalletRequest();
  unlockRequest.setPassphrase(passphrase);
  const unlockResponse = await walletServicePromiseClient.unlockWallet(unlockRequest, null);
  setUserData(unlockResponse.getRole(), unlockResponse.getOrganisationname());

  const status = await getWalletStatus();
  Logger.info(`status ${status}`);
  return status;
};

const MISSING = 'MISSING';
const UNLOCKED = 'UNLOCKED';
const LOCKED = 'LOCKED';
const WalletStatuses = {
  0: MISSING,
  1: UNLOCKED,
  2: LOCKED
};

const translateStatus = status => WalletStatuses[status];

export const isWalletUnlocked = async () => {
  const status = await getWalletStatus();
  return status === 1;
};

export const isIssuer = () => localStorage.getItem(USER_ROLE) === ISSUER;
