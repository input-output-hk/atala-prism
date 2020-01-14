/* eslint import/no-unresolved: 0 */ // --> OFF
import { ConnectorServicePromiseClient } from '../../protos/connector/connector_grpc_web_pb';
import {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest,
  SendMessageRequest
} from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';
import { getCredentialBinary } from '../credentials/credentialsManager';
import { getStudentById } from '../credentials/studentsManager';

const { SentCredential } = require('../../protos/credentials/credential_pb');

const { REACT_APP_GRPC_CLIENT, REACT_APP_VERIFIER, REACT_APP_ISSUER } = window._env_;
const connectorServiceClient = new ConnectorServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);
const issuerId = REACT_APP_ISSUER;

export const getConnectionsPaginated = (
  // Since the userId comes not from the session, I hardcoded it here
  userId = issuerId,
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
  const sentCredential = SentCredential.deserializeBinary(message.getMessage_asU8());
  const holderSentCredential = sentCredential.getHoldersentcredential();
  if (!holderSentCredential) return errorCredential;
  // In alpha version should be always a credential
  if (!holderSentCredential.hasCredential()) return errorCredential;
  return holderSentCredential.getCredential().toObject();
};

export const getMessagesForConnection = async (aUserId = REACT_APP_VERIFIER, connectionId) => {
  const userId = aUserId || REACT_APP_VERIFIER; // set default if null
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
      userId: issuerId
    })
    .catch(error => {
      Logger.error('Error issuing the credential: ', error);
      throw new Error(error);
    });
};
