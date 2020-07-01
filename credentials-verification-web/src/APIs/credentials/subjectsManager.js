import { SubjectsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { subjectToStudent, genericCredentialToStudentCredential } from '../helpers';
import { CONNECTION_ACCEPTED, HOLDER_PAGE_SIZE } from '../../helpers/constants';

import {
  GetSubjectRequest,
  GetSubjectsRequest,
  GenerateConnectionTokenForSubjectRequest,
  GetSubjectCredentialsRequest,
  CreateSubjectRequest
} from '../../protos/cmanager_api_pb';

function createAndPopulateGetSubjectRequest(limit, lastSeenSubjectId, groupName) {
  const getSubjectsRequest = new GetSubjectsRequest();

  getSubjectsRequest.setLimit(limit);
  getSubjectsRequest.setLastseensubjectid(lastSeenSubjectId);

  if (groupName) getSubjectsRequest.setGroupname(groupName);

  return getSubjectsRequest;
}

async function getSubjects(lastSeenCredentialId = null, limit = HOLDER_PAGE_SIZE, groupName) {
  Logger.info(
    `Getting the subjects for groupname: ${groupName}, from: ${lastSeenCredentialId}, limit: ${limit}`
  );
  const getSubjectsRequest = createAndPopulateGetSubjectRequest(
    limit,
    lastSeenCredentialId,
    groupName
  );

  const metadata = await this.auth.getMetadata(getSubjectsRequest);

  const result = await this.client.getSubjects(getSubjectsRequest, metadata);

  const { subjectsList } = result.toObject();
  // Should this be handled somewhere else?
  // will the displayed fields change in the future?
  return subjectsList.map(subjectToStudent);
}

async function getAllSubjects(groupName) {
  const allSubjects = [];
  const limit = 100;
  let response;
  // Since in the alpha version there will be only one group, all the created credentials
  // will belong to it. Therefore all the credentials must be created for every subject.
  // Since there's no way to know how many subjects are in the database, every time the
  // subjects are recieved it must be checked whether if those were all the remaining
  // ones.
  do {
    // This gets the id of the last subject so the backend can filter them
    const { id } = allSubjects.length ? allSubjects[allSubjects.length - 1] : {};

    // The next 100 subjects are requested
    // eslint-disable-next-line no-await-in-loop
    response = await this.getSubjectsAsIssuer(id, limit, groupName);
    const connectedSubjects = response.filter(
      ({ connectionstatus }) => connectionstatus === CONNECTION_ACCEPTED
    );

    allSubjects.push(...connectedSubjects);

    // If less than the requested subjects are returned it means all the subjects have
    // already been brought
  } while (response.length === limit);

  return allSubjects;
}

async function generateConnectionTokenAsIssuer(subjectId) {
  Logger.info(`Generating token for subjectId ${subjectId}`);
  const generateConnectionTokenRequest = new GenerateConnectionTokenForSubjectRequest();
  generateConnectionTokenRequest.setSubjectid(subjectId);

  const metadata = await this.auth.getMetadata(generateConnectionTokenRequest);

  const response = await this.client.generateConnectionTokenForSubject(
    generateConnectionTokenRequest,
    metadata
  );

  return response.getToken();
}

async function getSubjectById(subjectId) {
  const getSubjectRequest = new GetSubjectRequest();
  getSubjectRequest.setSubjectid(subjectId);

  const metadata = await this.auth.getMetadata(getSubjectRequest);

  const result = await this.client.getSubject(getSubjectRequest, metadata);

  const { subject } = result.toObject();

  return subject;
}

async function getSubjectCredentials(subjectId) {
  Logger.info('Getting credentials for the subject: ', subjectId);

  try {
    const getSubjectCredentialsRequest = new GetSubjectCredentialsRequest();
    getSubjectCredentialsRequest.setSubjectid(subjectId);

    const metadata = await this.auth.getMetadata(getSubjectCredentialsRequest);

    const response = await this.client.getSubjectCredentials(
      getSubjectCredentialsRequest,
      metadata
    );
    const { genericcredentialsList } = response.toObject();

    return genericcredentialsList.map(genericCredentialToStudentCredential);
  } catch (e) {
    Logger.error(
      'An error ocurred while getting the credentials for subject',
      subjectId,
      '\n Error:',
      e
    );
    throw new Error(e);
  }
}

async function createSubject(groupName, jsondata) {
  const request = new CreateSubjectRequest();
  request.setJsondata(JSON.stringify(jsondata));
  request.setGroupname(groupName);

  const metadata = await this.auth.getMetadata(request);

  const subject = await this.client.createSubject(request, metadata);

  Logger.info('Created subject:', subject.toObject());
}

function SubjectsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new SubjectsServicePromiseClient(this.config.grpcClient, null, null);
}

SubjectsManager.prototype.getSubjectsAsIssuer = getSubjects;
SubjectsManager.prototype.generateConnectionTokenAsIssuer = generateConnectionTokenAsIssuer;
SubjectsManager.prototype.getSubjectById = getSubjectById;
SubjectsManager.prototype.getSubjectCredentials = getSubjectCredentials;
SubjectsManager.prototype.createSubject = createSubject;
SubjectsManager.prototype.getAllSubjects = getAllSubjects;

export default SubjectsManager;
