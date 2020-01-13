/* eslint import/no-unresolved: 0 */ // --> OFF
import { ConnectorServicePromiseClient } from '../../protos/connector/connector_grpc_web_pb';
import {
  GetConnectionsPaginatedRequest,
  GetMessagesForConnectionRequest,
  SendMessageRequest,
  DeleteConnectionRequest
} from '../../protos/connector/connector_pb';
import Logger from '../../helpers/Logger';
import { getCredentialBinary } from '../credentials/credentialsManager';
import { getStudentById } from '../credentials/studentsManager';
import { HARDCODED_LIMIT } from '../../helpers/constants';

const { SentCredential } = require('../../protos/credentials/credential_pb');

const { REACT_APP_GRPC_CLIENT, REACT_APP_VERIFIER, REACT_APP_ISSUER_ID } = window._env_;
const connectorServiceClient = new ConnectorServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);
const issuerId = REACT_APP_ISSUER_ID;

export const issueCredential = async credentialData => {
  const sendMessageRequest = new SendMessageRequest();

  const { studentId } = credentialData;

  const studentData = await getStudentById(studentId);

  const credentialBinary = await getCredentialBinary(credentialData, studentData);

  const { connectionid } = studentData;
  sendMessageRequest.setConnectionid(connectionid);
  sendMessageRequest.setMessage(credentialBinary);

  return connectorServiceClient
    .sendMessage(sendMessageRequest, {
      userId: issuerId
    })
    .then(() => deleteConnection(connectionid))
    .catch(error => {
      Logger.error('Error issuing the credential: ', error);
      throw new Error(error);
    });
};

export const deleteConnection = connectionId => {
  const request = new DeleteConnectionRequest();

  request.setConnectionid(connectionId);

  return connectorServiceClient.deleteConnection(request, { userId: issuerId });
};
