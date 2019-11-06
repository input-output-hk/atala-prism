import grpcWeb from 'grpc-web';
import { WalletServiceClient } from '../../protos/wallet/wallet_grpc_web_pb';
import { GetDIDRequest } from '../../protos/wallet/wallet_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = process.env;
const walletServiceClient = new WalletServiceClient(REACT_APP_GRPC_CLIENT, null, null);

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
