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
import { USER_ROLE, ORGANISATION_NAME, ISSUER, VERIFIER, LOGO } from '../../helpers/constants';

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

const createAndPopulateRequest = ({ passphrase, organisationName, role, file }) => {
  const createWalletRequest = new CreateWalletRequest();
  const encodedLogo = new TextEncoder().encode(file);

  const roleDictionary = {
    ISSUER: 0,
    VERIFIER: 1
  };

  createWalletRequest.setPassphrase(passphrase);
  createWalletRequest.setOrganisationname(organisationName);
  createWalletRequest.setRole(roleDictionary[role]);
  createWalletRequest.setLogo(encodedLogo);

  return createWalletRequest;
};

export const createWallet = async (passphrase, organisationName, role, file) => {
  try {
    const createWalletRequest = createAndPopulateRequest({
      passphrase,
      organisationName,
      role,
      file
    });

    const response = await walletServicePromiseClient.createWallet(createWalletRequest);

    return response.toObject();
  } catch (e) {
    Logger.info('Error at wallet creation', e);
    throw new Error(e);
  }
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

const setUserData = ({ role, organisationname, logo }) => {
  const roleAsString = translateUserRole(role);
  localStorage.setItem(USER_ROLE, roleAsString);
  localStorage.setItem(ORGANISATION_NAME, organisationname);
  localStorage.setItem(LOGO, logo);
};

export const unlockWallet = async passphrase => {
  const unlockRequest = new UnlockWalletRequest();
  unlockRequest.setPassphrase(passphrase);
  const unlockResponse = await walletServicePromiseClient.unlockWallet(unlockRequest, null);
  setUserData(unlockResponse.toObject());

  const status = await getWalletStatus();
  Logger.info(`status ${status}`);
  return status;
};

export const isWalletUnlocked = async () => {
  const status = await getWalletStatus();
  return status === 1;
};

export const isIssuer = () => localStorage.getItem(USER_ROLE) === ISSUER;
