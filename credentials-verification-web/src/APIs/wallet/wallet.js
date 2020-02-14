import { WalletServicePromiseClient } from '../../protos/wallet/wallet_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { USER_ROLE, ORGANISATION_NAME, ISSUER, VERIFIER, LOGO } from '../../helpers/constants';

const {
  CreateWalletRequest,
  GetDIDRequest,
  LockWalletRequest,
  UnlockWalletRequest,
  GetWalletStatusRequest
} = require('../../protos/wallet/wallet_pb');

const { config } = require('../config');

const walletServicePromiseClient = new WalletServicePromiseClient(
  config.walletGrpcClient,
  null,
  null
);

export const getDid = async () => {
  const didRequest = new GetDIDRequest();

  const response = await walletServicePromiseClient.getDID(didRequest, {});

  const { did } = response.toObject();

  return did;
};

const createAndPopulateRequest = ({ passphrase, organisationName, role, file }) => {
  const createWalletRequest = new CreateWalletRequest();
  const binaryFile = new Uint8Array(file);

  const roleDictionary = {
    ISSUER: 0,
    VERIFIER: 1
  };

  createWalletRequest.setPassphrase(passphrase);
  createWalletRequest.setOrganisationname(organisationName);
  createWalletRequest.setRole(roleDictionary[role]);
  createWalletRequest.setLogo(binaryFile);

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

    // This returns a signed operation that is not being used yet
    await walletServicePromiseClient.createWallet(createWalletRequest);

    return getDid();
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
  return status;
};

export const isWalletUnlocked = async () => {
  const status = await getWalletStatus();
  return status === 1;
};

export const isIssuer = () => localStorage.getItem(USER_ROLE) === ISSUER;
