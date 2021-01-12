import { ConnectorServicePromiseClient } from '../../protos/connector_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import {
  GetConnectionsPaginatedRequest,
  SendMessageRequest,
  RegisterDIDRequest
} from '../../protos/connector_api_pb';

async function getConnectionsPaginated(lastSeenConnectionId, limit) {
  const connectionsPaginatedRequest = new GetConnectionsPaginatedRequest();

  connectionsPaginatedRequest.setLastseenconnectionid(lastSeenConnectionId);
  connectionsPaginatedRequest.setLimit(limit);

  const metadata = await this.auth.getMetadata(connectionsPaginatedRequest);

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

  sendMessageRequest.setConnectionid(connectionId);
  sendMessageRequest.setMessage(message);

  const metadata = await this.auth.getMetadata(sendMessageRequest);

  return this.client.sendMessage(sendMessageRequest, metadata).catch(error => {
    Logger.error('Error issuing the credential: ', error);
    throw new Error(error);
  });
}

async function registerUser(createOperation, name, logoFile, isIssuer) {
  const registerRequest = new RegisterDIDRequest();
  const logo = new TextEncoder().encode(logoFile);

  registerRequest.setRole(
    isIssuer ? RegisterDIDRequest.Role.ISSUER : RegisterDIDRequest.Role.VERIFIER
  );
  registerRequest.setName(name);
  registerRequest.setLogo(logo);
  registerRequest.setCreatedidoperation(createOperation);

  const response = await this.client.registerDID(registerRequest);

  const { id } = response.toObject();

  return id;
}

function Connector(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConnectorServicePromiseClient(config.grpcClient, null, null);
}

Connector.prototype.getConnectionsPaginated = getConnectionsPaginated;
Connector.prototype.sendCredential = sendCredential;
Connector.prototype.registerUser = registerUser;

export default Connector;
