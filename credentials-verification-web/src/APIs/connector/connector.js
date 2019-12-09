/* eslint import/no-unresolved: 0 */ // --> OFF
import { ConnectorServicePromiseClient } from '../../protos/connector/connector_grpc_web_pb';
import {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest
} from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';

const { SentCredential } = require('../../protos/credentials/credential_pb');

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

const mapMessageToCredential = message => {
  const sentCredential = SentCredential.deserializeBinary(message.getMessage_asU8());
  const holderSentCredential = sentCredential.getHoldersentcredential();
  if (!holderSentCredential) return errorCredential;
  // In alpha version should be always a credential
  if (!holderSentCredential.hasCredential()) return errorCredential;
  return holderSentCredential.getCredential().toObject();
};

export const getMessagesForConnection = async (userId, connectionId) => {
  Logger.info(`Getting messages for connectionId ${connectionId}`);
  const request = new GetMessagesForConnectionRequest();
  request.setConnectionid(connectionId);
  const result = await connectorServiceClient.getMessagesForConnection(request, {
    userId: 'c8834532-eade-11e9-a88d-d8f2ca059830' // Since the userId comes not from the session, I hardcoded it here
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
    surnameList: ['Error']
  }
};
