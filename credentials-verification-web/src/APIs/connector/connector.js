import grpcWeb from 'grpc-web';
import { ConnectorServiceClient } from '../../protos/connector/connector_grpc_web_pb';
import {
  GenerateConnectionTokenRequest,
  GetConnectionsPaginatedRequest
} from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = process.env;
const issuerId = 'c8834532-eade-11e9-a88d-d8f2ca059830';
const connectorServiceClient = new ConnectorServiceClient(REACT_APP_GRPC_CLIENT, null, null);

const generateConnectionTokenCallback = callback => (error, response) => {
  if (error) return Logger.error('An error: ', error);
  Logger.info('This is the response', response.getToken());
  callback(response.getToken());
};

export const generateConnectionToken = (userId, callback) => {
  const generateConnectionTokenRequest = new GenerateConnectionTokenRequest();
  const call = connectorServiceClient.generateConnectionToken(
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

  connectorServiceClient.call(
    connectionsPaginatedRequest,
    { userId },
    getConnectionsPaginatedCallback
  );
};
