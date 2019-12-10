/* eslint import/no-unresolved: 0 */ // --> OFF
import { CredentialsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import {
  GetCredentialsRequest,
  CreateCredentialRequest,
  Date
} from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';
import { setDateInfoFromJSON } from '../helpers';
import { getStudents } from './studentsManager';

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
  studentId,
  title,
  enrollmentDate,
  graduationDate,
  groupName
) => {
  const createCredentialRequest = new CreateCredentialRequest();

  createCredentialRequest.setStudentid(studentId);
  createCredentialRequest.setTitle(title);
  createCredentialRequest.setEnrollmentdate(enrollmentDate);
  createCredentialRequest.setGraduationdate(graduationDate);
  createCredentialRequest.setGroupname(groupName);

  return createCredentialRequest;
};

const getAllStudents = async () => {
  const allStudents = [];
  const limit = 100;
  let response;

  // Since in the alpha version there will be only one group, all the created credentials
  // will belong to it. Therefore all the credentials must be created for every student.
  // Since there's no way to know how many students are in the database, every time the
  // students are recieved it must be checked whether if those were all the remaining
  // ones.
  do {
    // This gets the id of the last student so the backend can filter them
    const { id } = allStudents.length ? allStudents[allStudents.length - 1] : {};

    // The next 100 students are requested
    // eslint-disable-next-line no-await-in-loop
    response = await getStudents(limit, id);
    allStudents.push(...response);

    // If less than the requested students are returned it means all the students have
    // already been brought
  } while (response.length === limit);

  return allStudents;
};

export const createCredential = async ({ title, enrollmentDate, graduationDate, groupName }) => {
  Logger.info('Creating credentials for the all the subjects as the issuer: ', issuerId);

  const enrollmentDateObject = new Date();
  const graduationDateObject = new Date();

  setDateInfoFromJSON(enrollmentDateObject, enrollmentDate);
  setDateInfoFromJSON(graduationDateObject, graduationDate);

  const allStudents = await getAllStudents();

  const credentialStudentsPromises = allStudents.map(student => {
    const createCredentialRequest = createAndPopulateCreationRequest(
      student.id,
      title,
      enrollmentDateObject,
      graduationDateObject,
      groupName
    );

    return credentialsService.createCredential(createCredentialRequest, {
      userId: issuerId
    });
  });

  return Promise.all(credentialStudentsPromises);
};
