/* eslint import/no-unresolved: 0 */ // --> OFF
import { CredentialsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import {
  GetCredentialsRequest,
  CreateCredentialRequest,
  Date
} from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';
import { setDateInfoFromJSON } from '../helpers';

const { REACT_APP_GRPC_CLIENT } = window._env_;
const issuerId = 'c8834532-eade-11e9-a88d-d8f2ca059830';
const credentialsService = new CredentialsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

export const getCredentials = async (limit = 10, lastSeenCredentialId = null) => {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);
  const getCredentialsRequest = new GetCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  const result = await credentialsService.getCredentials(getCredentialsRequest, {
    userId: issuerId
  });
  const { credentialsList } = result.toObject();
  return { credentials: credentialsList, count: credentialsList.length };
};

const createAndPopulateCreationRequest = (
  subject,
  title,
  enrollmentDate,
  graduationDate,
  groupName
) => {
  const createCredentialRequest = new CreateCredentialRequest();

  createCredentialRequest.setSubject(subject);
  createCredentialRequest.setTitle(title);
  createCredentialRequest.setEnrollmentdate(enrollmentDate);
  createCredentialRequest.setGraduationdate(graduationDate);
  createCredentialRequest.setGroupname(groupName);

  return createCredentialRequest;
};

export const createCredential = ({ subject, title, enrollmentDate, graduationDate, groupName }) => {
  Logger.info('Creating credential for the subject: ', subject, 'as the issuer: ', issuerId);

  const enrollmentDateObject = new Date();
  const graduationDateObject = new Date();

  setDateInfoFromJSON(enrollmentDateObject, enrollmentDate);
  setDateInfoFromJSON(graduationDateObject, graduationDate);

  const createCredentialRequest = createAndPopulateCreationRequest(
    subject,
    title,
    enrollmentDateObject,
    graduationDateObject,
    groupName
  );

  return credentialsService.createCredential(createCredentialRequest, {
    userId: issuerId
  });
};
