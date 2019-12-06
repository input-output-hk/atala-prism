/* eslint import/no-unresolved: 0 */ // --> OFF
import grpcWeb from 'grpc-web';
import {
  WalletServiceClient,
  WalletServicePromiseClient
} from '../../protos/wallet/wallet_grpc_web_pb';
import {
  CreateWalletRequest,
  GetDIDRequest,
  UnlockWalletRequest,
  GetWalletStatusRequest
} from '../../protos/wallet/wallet_pb';
import Logger from '../../helpers/Logger';
import { ISSUER } from '../../helpers/constants';

const { REACT_APP_GRPC_CLIENT } = window._env_;
const walletServiceClient = new WalletServiceClient(REACT_APP_GRPC_CLIENT, null, null);
const walletServicePromiseClient = new WalletServicePromiseClient(
  REACT_APP_GRPC_CLIENT,
  null,
  null
);

const getDidCallback = (error, response) => {
  if (error) return Logger.error('An error: ', error);
  Logger.info('This is the response', response.getDid());
};

export const getDid = () => {
  const didRequest = new GetDIDRequest();

  const call = walletServiceClient.getDID(didRequest, {}, getDidCallback);

  call.on('status', status => {
    if (status.code !== grpcWeb.StatusCode.OK) {
      Logger.error('Error code: ' + status.code + ' "' + status.details + '"');
    }

    if (status.metadata) {
      Logger.info('Received metadata');
      Logger.info(status.metadata);
    }
  });
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

export const unlockWallet = async passphrase => {
  const unlockRequest = new UnlockWalletRequest();
  unlockRequest.setPassphrase(passphrase);
  /* const unlockResponse = */ await walletServiceClient.unlockWallet(unlockRequest, null);
  // const response = new UnlockWalletResponse(unlockResponse);
  // console.log('unlockResponse', response, 'as obj', response.toObject());
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

export const isIssuer = () => localStorage.getItem('userRole') === ISSUER;
