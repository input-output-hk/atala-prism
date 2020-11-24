import { ConnectorServicePromiseClient } from '../../protos/connector_api_grpc_web_pb';
import Logger from '../../helpers/Logger';

const {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest,
  SendMessageRequest,
  RegisterDIDRequest,
  GetMessagesPaginatedRequest
} = require('../../protos/connector_api_pb');

const { AtalaMessage, Credential } = require('../../protos/credential_models_pb');

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

async function mapMessageToCredential(message) {
  const sentCredential = AtalaMessage.deserializeBinary(message.getMessage_asU8());
  const holderSentCredential = sentCredential.getHoldersentcredential();
  if (!holderSentCredential) return errorCredential;
  // In alpha version should be always a credential
  if (!holderSentCredential.hasCredential()) return errorCredential;
  return holderSentCredential.getCredential().toObject();
}

function getCredentialFromMessage(message) {
  const credDoc = Credential.deserializeBinary(message.getMessage_asU8()).getCredentialdocument();
  return JSON.parse(credDoc);
}

async function getMessagesForConnection(connectionId) {
  Logger.info(`Getting messages for connectionId ${connectionId}`);
  const request = new GetMessagesForConnectionRequest();
  request.setConnectionid(connectionId);

  const metadata = await this.auth.getMetadata(request);
  const result = await this.client.getMessagesForConnection(request, metadata);

  // TODO: we should use mapMessageToCredential and update HolderSentCredential to contain a Credential
  // instead of an AlphaCredential. getCredentialFromMessage is only used as mobile apps are currently
  // not wrapping the Credential in an AtalaMessage
  return result.getMessagesList().map(msg => getCredentialFromMessage(msg));
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

async function getCredentialsReceived(limit, lastSeenMessageId = null) {
  Logger.info(`getting credentials received from ${lastSeenMessageId}, limit ${limit}`);

  const getCredentialsReceivedRequest = new GetMessagesPaginatedRequest();
  getCredentialsReceivedRequest.setLimit(limit);
  getCredentialsReceivedRequest.setLastseenmessageid(lastSeenMessageId);

  const metadata = await this.auth.getMetadata(getCredentialsReceivedRequest);

  const result = await this.client.getMessagesPaginated(getCredentialsReceivedRequest, metadata);

  return result.getMessagesList().map(msg => getCredentialFromMessage(msg));
}

function Connector(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConnectorServicePromiseClient(config.grpcClient, null, null);
}

Connector.prototype.getConnectionsPaginated = getConnectionsPaginated;
Connector.prototype.getMessagesForConnection = getMessagesForConnection;
Connector.prototype.sendCredential = sendCredential;
Connector.prototype.registerUser = registerUser;
Connector.prototype.getCredentialsReceived = getCredentialsReceived;

export default Connector;
