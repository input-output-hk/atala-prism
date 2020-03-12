import { ConnectorServicePromiseClient } from '../../protos/connector_api_grpc_web_pb';
import Logger from '../../helpers/Logger';

const {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest,
  SendMessageRequest,
  RegisterDIDRequest
} = require('../../protos/connector_api_pb');

const { AtalaMessage } = require('../../protos/credential_pb');

async function getConnectionsPaginated(lastSeenConnectionId, limit) {
  const connectionsPaginatedRequest = new GetConnectionsPaginatedRequest();

  connectionsPaginatedRequest.setLastseenconnectionid(lastSeenConnectionId);
  connectionsPaginatedRequest.setLimit(limit);

  return this.client
    .getConnectionsPaginated(connectionsPaginatedRequest, this.auth.getMetadata())
    .then(call => {
      const { connectionsList } = call.toObject();
      return connectionsList;
    })
    .catch(error => Logger.error('An error: ', error));
}

async function mapMessageToCredential(message) {
  const sentCredential = AtalaMessage.deserializeBinary(message.getMessage_asU8());
  const holderSentCredential = sentCredential.getHoldersentcredential();
  if (!holderSentCredential) return errorCredential;
  // In alpha version should be always a credential
  if (!holderSentCredential.hasCredential()) return errorCredential;
  return holderSentCredential.getCredential().toObject();
}

async function getMessagesForConnection(connectionId) {
  Logger.info(`Getting messages for connectionId ${connectionId}`);
  const request = new GetMessagesForConnectionRequest();
  request.setConnectionid(connectionId);
  const result = await this.client.getMessagesForConnection(request, this.auth.getMetadata());
  return result.getMessagesList().map(msg => mapMessageToCredential(msg));
}

const errorCredential = {
  additionalspeciality: '',
  admissiondate: { year: 1970, month: 1, day: 1 },
  graduationdate: { year: 1970, month: 1, day: 1 },
  degreeawarded: 'Error',
  issuertype: { academicauthority: 'Error', did: 'error', issuerlegalname: 'Error', issuertype: 0 },
  subjectdata: {
    dateofbirth: undefined,
    iddocument: undefined,
    namesList: ['Error'],
    surnamesList: ['Error']
  }
};

async function issueCredential(message, connectionId) {
  const sendMessageRequest = new SendMessageRequest();

  sendMessageRequest.setConnectionid(connectionId);
  sendMessageRequest.setMessage(message);

  return this.client.sendMessage(sendMessageRequest, this.auth.getMetadata()).catch(error => {
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

  const response = await this.client.registerDID(registerRequest, this.auth.getMetadata());

  const { id } = response.toObject();

  return id;
}

function Connector(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConnectorServicePromiseClient(config.grpcClient, null, null);
}

Connector.prototype.getConnectionsPaginated = getConnectionsPaginated;
Connector.prototype.getMessagesForConnection = getMessagesForConnection;
Connector.prototype.issueCredential = issueCredential;
Connector.prototype.registerUser = registerUser;

export default Connector;
