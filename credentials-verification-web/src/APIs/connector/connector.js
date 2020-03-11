import { ConnectorServicePromiseClient } from '../../protos/connector_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { getCredentialBinary } from '../credentials/credentialsManager';
import { getStudentById } from '../credentials/studentsManager';

const {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest,
  SendMessageRequest
} = require('../../protos/connector_api_pb');

const { AtalaMessage } = require('../../protos/credential_pb');

const { config } = require('../config');

const connectorServiceClient = new ConnectorServicePromiseClient(config.grpcClient, null, null);

export const getConnectionsPaginated = (
  // Since the userId comes not from the session, I hardcoded it here
  userId = config.issuerId,
  lastSeenConnectionId,
  limit
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

const mapMessageToCredential = message => {
  const sentCredential = AtalaMessage.deserializeBinary(message.getMessage_asU8());
  const holderSentCredential = sentCredential.getHoldersentcredential();
  if (!holderSentCredential) return errorCredential;
  // In alpha version should be always a credential
  if (!holderSentCredential.hasCredential()) return errorCredential;
  return holderSentCredential.getCredential().toObject();
};

export const getMessagesForConnection = async (aUserId = config.verifierId, connectionId) => {
  const userId = aUserId || config.verifierId; // set default if null
  Logger.info(`Getting messages for connectionId ${connectionId} and userId ${userId}`);
  const request = new GetMessagesForConnectionRequest();
  request.setConnectionid(connectionId);
  const result = await connectorServiceClient.getMessagesForConnection(request, {
    userId
  });
  return result.getMessagesList().map(msg => mapMessageToCredential(msg));
};

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

export const issueCredential = async credentialData => {
  const sendMessageRequest = new SendMessageRequest();

  const { studentid } = credentialData;

  const studentData = await getStudentById(studentid);

  const credentialBinary = await getCredentialBinary(credentialData, studentData);

  const { connectionid } = studentData;
  sendMessageRequest.setConnectionid(connectionid);
  sendMessageRequest.setMessage(credentialBinary);

  return connectorServiceClient
    .sendMessage(sendMessageRequest, {
      userId: config.issuerId
    })
    .catch(error => {
      Logger.error('Error issuing the credential: ', error);
      throw new Error(error);
    });
};
