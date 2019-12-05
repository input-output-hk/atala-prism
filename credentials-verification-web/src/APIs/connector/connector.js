/* eslint import/no-unresolved: 0 */ // --> OFF
import { ConnectorServicePromiseClient } from '../../protos/connector/connector_grpc_web_pb';
import { GetConnectionsPaginatedRequest, GetMessagesForConnectionRequest } from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = window._env_;
const connectorServiceClient = new ConnectorServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

export const getConnectionsPaginated = (
  // Since the userId comes not from the session, I hardcoded it here
  userId = 'c8834532-eade-11e9-a88d-d8f2ca059830',
  lastSeenConnectionId,
  limit = 10
) => {
  const connectionsPaginatedRequest = new GetConnectionsPaginatedRequest();

  connectionsPaginatedRequest.setLastseenconnectionid(lastSeenConnectionId);
  connectionsPaginatedRequest.setLimit(limit);

  return connectorServiceClient
    .getConnectionsPaginated(connectionsPaginatedRequest, { userId })
    .then(call => {
      const { connectionsList } = call.toObject();
      return connectionsList;
    })
    .catch(error => Logger.error('An error: ', error));
};

export const getMessagesForConnection = async (userId, connectionId) => {
  const request = new GetMessagesForConnectionRequest();
  request.setConnectionid(connectionId);

  const result = await connectorServiceClient.getMessagesForConnection(request, { userId });
  Logger.info(result.getMessagesList());
  result.getMessagesList().map(msg => Object.assign({}, msg, { message: })) // TODO parse message
  return result.getMessagesList();
};
