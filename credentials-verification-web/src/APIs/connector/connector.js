import grpcWeb from 'grpc-web';
import { ConnectorServiceClient } from '../../protos/connector/connector_grpc_web_pb';
import {
  GenerateConnectionTokenRequest,
  GetConnectionsPaginatedRequest
} from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = process.env;
const issuerId = 'c8834532-eade-11e9-a88d-d8f2ca059830';

const generateConnectionTokenCallback = callback => (error, response) => {
  if (error) return Logger.error('An error: ', error);
  Logger.info('This is the response', response.getToken());
  callback(response.getToken());
};

export const geConnectionToken = (userId, callback) => {
  const service = new ConnectorServiceClient(REACT_APP_GRPC_CLIENT, null, null);

  const generateConnectionTokenRequest = new GenerateConnectionTokenRequest();
  const call = service.generateConnectionToken(
    generateConnectionTokenRequest,
    { userId: issuerId }, // TODO unhardcode this when there be more user ids
    generateConnectionTokenCallback(callback)
  );

  call.on('status', status => {
    if (status.code !== grpcWeb.StatusCode.OK) {
      Logger.error('Error code: ' + status.code + ' "' + status.details + '"');
    }
  });
};

const getConnectionsPaginatedCallback = (error, response) => {
  if (error) return Logger.error('An error: ', error);
  Logger.info('This is the response', response.getConnections());
};

export const getConnectionsPaginated = (userId, lastSeenConnectionId, limit = 10) => {
  const connectionsPaginatedRequest = new GetConnectionsPaginatedRequest(
    lastSeenConnectionId,
    limit
  );

  const service = new ConnectorServiceClient(`${REACT_APP_GRPC_CLIENT}`, null, null);
  service.call(connectionsPaginatedRequest, { userId }, getConnectionsPaginatedCallback);
};
