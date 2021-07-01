import { ConnectorServicePromiseClient } from '../../protos/connector_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import {
  GetConnectionsPaginatedRequest,
  SendMessageRequest,
  SendMessagesRequest
} from '../../protos/connector_api_pb';
import { MessageToSendByConnectionToken } from '../../protos/connector_models_pb';
import { BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } from '../../helpers/constants';

async function getConnectionsPaginated(lastSeenConnectionId, limit) {
  const connectionsPaginatedRequest = new GetConnectionsPaginatedRequest();

  connectionsPaginatedRequest.setLastSeenConnectionId(lastSeenConnectionId);
  connectionsPaginatedRequest.setLimit(limit);

  const { metadata, sessionError } = await this.auth.getMetadata(
    connectionsPaginatedRequest,
    BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS
  );
  if (sessionError) return [];

  return this.client
    .getConnectionsPaginated(connectionsPaginatedRequest, metadata)
    .then(call => {
      const { connectionsList } = call.toObject();
      return connectionsList;
    })
    .catch(error => Logger.error('An error: ', error));
}

async function sendCredential(message, connectionId) {
  const sendMessageRequest = new SendMessageRequest();

  sendMessageRequest.setConnectionId(connectionId);
  sendMessageRequest.setMessage(message);

  const { metadata, sessionError } = await this.auth.getMetadata(sendMessageRequest);
  if (sessionError) return;

  return this.client.sendMessage(sendMessageRequest, metadata).catch(error => {
    Logger.error('Error issuing the credential: ', error);
    throw error;
  });
}

async function sendCredentialsBulk(payload) {
  const messagesToBeSent = payload.map(({ connectionToken, atalaMessage }) => {
    const messageToBeSent = new MessageToSendByConnectionToken();

    messageToBeSent.setConnectionToken(connectionToken);
    messageToBeSent.setMessage(atalaMessage);
    return messageToBeSent;
  });

  const sendMessagesRequest = new SendMessagesRequest();
  sendMessagesRequest.setMessagesByConnectionTokenList(messagesToBeSent);

  const { metadata, sessionError } = await this.auth.getMetadata(sendMessagesRequest);
  if (sessionError) return;

  return this.client.sendMessages(sendMessagesRequest, metadata).catch(error => {
    Logger.error('Error issuing the credential: ', error);
    throw error;
  });
}

function Connector(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConnectorServicePromiseClient(config.grpcClient, null, null);
}

Connector.prototype.getConnectionsPaginated = getConnectionsPaginated;
Connector.prototype.sendCredential = sendCredential;
Connector.prototype.sendCredentialsBulk = sendCredentialsBulk;

export default Connector;
