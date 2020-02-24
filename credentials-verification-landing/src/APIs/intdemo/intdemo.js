import { IDServicePromiseClient } from '../../protos/demo/intdemo_grpc_web_pb';
import { config } from '../configs';
import Logger from '../../helpers/Logger';

const {
  GetConnectionTokenRequest,
  GetSubjectStatusRequest
} = require('../../protos/demo/intdemo_grpc_web_pb');

const idService = new IDServicePromiseClient(config.grpcClient, null, null);
const getUserId = () => config.issuerId;

export const getConnectionToken = async () => {
  Logger.info('getting connection token');
  const request = new GetConnectionTokenRequest();
  const result = await idService.getConnectionToken(request, { userId: getUserId() });
  return result.getConnectiontoken();
};

export const startSubjectStatusStream = (connectionToken, onData, onClosed) => {
  Logger.info('Getting subject status', connectionToken);
  const request = new GetSubjectStatusRequest();
  request.setConnectiontoken(connectionToken);
  const stream = idService.getSubjectStatusStream(request, { userId: getUserId() });
  stream.on('data', response => {
    Logger.info('stream data:', response);
    Logger.info(response.getSubjectstatus());
    onData(response.getSubjectstatus());
  });
  stream.on('end', end => {
    Logger.info('ending stream:', end);
    if (onClosed) onClosed(end);
  });
};
