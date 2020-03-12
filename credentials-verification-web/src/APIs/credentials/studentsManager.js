import { StudentsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { setDateInfoFromJSON } from '../helpers';
import { CONNECTION_ACCEPTED, HOLDER_PAGE_SIZE } from '../../helpers/constants';

const { Date } = require('../../protos/common_models_pb');
const {
  GetStudentRequest,
  GetStudentsRequest,
  GenerateConnectionTokenForStudentRequest,
  GetStudentCredentialsRequest,
  CreateStudentRequest
} = require('../../protos/cmanager_api_pb');

function createAndPopulateGetStudentRequest(limit, lastSeenStudentId, groupName) {
  const getStudentsRequest = new GetStudentsRequest();

  getStudentsRequest.setLimit(limit);
  getStudentsRequest.setLastseenstudentid(lastSeenStudentId);

  if (groupName) getStudentsRequest.setGroupname(groupName);

  return getStudentsRequest;
}

async function getStudents(lastSeenCredentialId = null, limit = HOLDER_PAGE_SIZE, groupName) {
  Logger.info('Getting the students');
  const getStudentsRequest = createAndPopulateGetStudentRequest(
    limit,
    lastSeenCredentialId,
    groupName
  );

  const result = await this.client.getStudents(getStudentsRequest, this.auth.getMetadata());

  const { studentsList } = result.toObject();

  return studentsList;
}

async function getAllStudents(groupName) {
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
    response = await this.getIndividualsAsIssuer(id, limit, groupName);
    const connectedStudents = response.filter(
      ({ connectionstatus }) => connectionstatus === CONNECTION_ACCEPTED
    );

    allStudents.push(...connectedStudents);

    // If less than the requested students are returned it means all the students have
    // already been brought
  } while (response.length === limit);

  return allStudents;
}

async function generateConnectionTokenAsIssuer(studentId) {
  Logger.info(`Generating token for studentId ${studentId}`);
  const generateConnectionTokenRequest = new GenerateConnectionTokenForStudentRequest();
  generateConnectionTokenRequest.setStudentid(studentId);
  const response = await this.client.generateConnectionTokenForStudent(
    generateConnectionTokenRequest,
    this.auth.getMetadata()
  );

  return response.getToken();
}

async function getStudentById(studentId) {
  const getStudentRequest = new GetStudentRequest();
  getStudentRequest.setStudentid(studentId);

  const result = await this.client.getStudent(getStudentRequest, this.auth.getMetadata());

  const { student } = result.toObject();

  return student;
}

async function getStudentCredentials(studentId) {
  Logger.info('Getting credentials for the student: ', studentId);

  try {
    const getStudentCredentialsRequest = new GetStudentCredentialsRequest();
    getStudentCredentialsRequest.setStudentid(studentId);

    const response = await this.client.getStudentCredentials(
      getStudentCredentialsRequest,
      this.auth.getMetadata()
    );
    const { credentialList } = response.toObject();

    return credentialList;
  } catch (e) {
    Logger.error(
      'An error ocurred while getting the credentials for student',
      studentId,
      '\n Error:',
      e
    );
    throw new Error(e);
  }
}

async function createStudent({ studentId, fullName, email, admissionDate, groupName }) {
  const request = new CreateStudentRequest();
  const date = new Date();
  setDateInfoFromJSON(date, admissionDate);

  request.setUniversityassignedid(studentId);
  request.setFullname(fullName);
  request.setEmail(email);
  request.setAdmissiondate(date);
  request.setGroupname(groupName);

  const student = await this.client.createStudent(request, this.auth.getMetadata());

  Logger.info('Created student:', student.toObject());
}

function StudentsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new StudentsServicePromiseClient(this.config.grpcClient, null, null);
}

StudentsManager.prototype.getIndividualsAsIssuer = getStudents;
StudentsManager.prototype.generateConnectionTokenAsIssuer = generateConnectionTokenAsIssuer;
StudentsManager.prototype.getStudentById = getStudentById;
StudentsManager.prototype.getStudentCredentials = getStudentCredentials;
StudentsManager.prototype.createStudent = createStudent;
StudentsManager.prototype.getAllStudents = getAllStudents;

export default StudentsManager;
