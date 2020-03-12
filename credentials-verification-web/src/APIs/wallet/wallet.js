import { WalletServicePromiseClient } from '../../protos/wallet_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { ISSUER, VERIFIER } from '../../helpers/constants';

const {
  CreateWalletRequest,
  GetDIDRequest,
  LockWalletRequest,
  UnlockWalletRequest,
  GetWalletStatusRequest
} = require('../../protos/wallet_api_pb');

async function getDid() {
  const didRequest = new GetDIDRequest();

  const response = await this.client.getDID(didRequest, {});

  const { did } = response.toObject();

  return did;
}

function createAndPopulateRequest({ passphrase, organisationName, role, file }) {
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
}

async function createWallet(passphrase, organisationName, role, file) {
  try {
    const createWalletRequest = createAndPopulateRequest({
      passphrase,
      organisationName,
      role,
      file
    });

    const response = await this.client.createWallet(createWalletRequest);
    return response.getOperation();
  } catch (e) {
    Logger.info('Error at wallet creation', e);
    throw new Error(e);
  }
}

async function getWalletStatus() {
  const getWalletStatusRequest = new GetWalletStatusRequest();
  const walletStatus = await this.client.getWalletStatus(getWalletStatusRequest, null);
  return walletStatus.getStatus();
}

function cleanUserData(configs) {
  configs.userRole.remove();
  configs.organizationName.remove();
  configs.logo.remove();
}

async function lockWallet() {
  const lockRequest = new LockWalletRequest();
  await this.client.lockWallet(lockRequest, null);

  cleanUserData(this.config);
}

const UserRoles = {
  0: ISSUER,
  1: VERIFIER
};

function translateUserRole(role) {
  return UserRoles[role];
}

function setUserData({ role, organisationname, logo }, configs) {
  const roleAsString = translateUserRole(role);
  configs.userRole.set(roleAsString);
  configs.organizationName.set(organisationname);
  configs.logo.set(logo);
}

async function unlockWallet(passphrase) {
  const unlockRequest = new UnlockWalletRequest();
  unlockRequest.setPassphrase(passphrase);
  const unlockResponse = await this.client.unlockWallet(unlockRequest, null);
  setUserData(unlockResponse.toObject(), this.config);

  return this.getWalletStatus();
}

async function isWalletUnlocked() {
  const status = await this.getWalletStatus();
  return status === 1;
}

// TODO: When registering as issuer and then as verifier, the role is not updated.
function isIssuer() {
  return this.config.userRole.get() === ISSUER;
}

function getNonce() {
  // TODO implement
}

function signMessage(unsignedRequest, nonce) {
  // TODO implement
}

function Wallet(config) {
  this.config = config;
  this.client = new WalletServicePromiseClient(this.config.walletGrpcClient, null, null);
}

Wallet.prototype.getDid = getDid;
Wallet.prototype.createWallet = createWallet;
Wallet.prototype.getWalletStatus = getWalletStatus;
Wallet.prototype.lockWallet = lockWallet;
Wallet.prototype.unlockWallet = unlockWallet;
Wallet.prototype.isWalletUnlocked = isWalletUnlocked;
Wallet.prototype.isIssuer = isIssuer;
Wallet.prototype.getNonce = getNonce;
Wallet.prototype.signMessage = signMessage;

export default Wallet;
