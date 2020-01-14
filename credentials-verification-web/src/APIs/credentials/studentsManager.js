/* eslint import/no-unresolved: 0 */ // --> OFF
import { StudentsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import {
  GetStudentRequest,
  GetStudentsRequest,
  GenerateConnectionTokenRequest,
  GetStudentCredentialsRequest,
  CreateStudentRequest,
  Date
} from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';
import { isIssuer } from '../wallet/wallet';
import { setDateInfoFromJSON } from '../helpers';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';

const { REACT_APP_GRPC_CLIENT, REACT_APP_ISSUER, REACT_APP_VERIFIER } = window._env_;
const issuerId = REACT_APP_ISSUER;
const studentsService = new StudentsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

const createAndPopulateGetStudentRequest = (limit, lastSeenStudentId) => {
  const getStudentsRequest = new GetStudentsRequest();

  getStudentsRequest.setLimit(limit);
  getStudentsRequest.setLastseenstudentid(lastSeenStudentId);

  return getStudentsRequest;
};

export const getStudents = async (
  userId = REACT_APP_ISSUER,
  lastSeenCredentialId = null,
  limit = HOLDER_PAGE_SIZE
) => {
  Logger.info('Getting the students');
  const getStudentsRequest = createAndPopulateGetStudentRequest(limit, lastSeenCredentialId);

  const result = await studentsService.getStudents(getStudentsRequest, { userId });

  const { studentsList } = result.toObject();

  return studentsList;
};

export const generateConnectionToken = async (userId, studentId) => {
  const hardCodedUserId = isIssuer() ? REACT_APP_ISSUER : REACT_APP_VERIFIER;
  Logger.info(`Generating token for studentId ${studentId}`);
  const generateConnectionTokenRequest = new GenerateConnectionTokenRequest();
  generateConnectionTokenRequest.setStudentid(studentId);
  const response = await studentsService.generateConnectionToken(
    generateConnectionTokenRequest,
    { userId: hardCodedUserId } // TODO unhardcode this when there be more user ids
  );

  return response.getToken();
};

export const getStudentById = async studentId => {
  const getStudentRequest = new GetStudentRequest();
  getStudentRequest.setStudentid(studentId);

  const result = await studentsService.getStudent(getStudentRequest, { userId: issuerId });

  const { student } = result.toObject();

  return student;
};

export const getStudentCredentials = async (studentId, issuer = issuerId) => {
  Logger.info('Getting credentials for the student: ', studentId, 'as the issuer: ', issuer);

  try {
    const getStudentCredentialsRequest = new GetStudentCredentialsRequest();
    getStudentCredentialsRequest.setStudentid(studentId);

    const response = await studentsService.getStudentCredentials(getStudentCredentialsRequest, {
      userId: issuer
    });
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
};

export const createStudent = async (universityAssignedId, fullName, email, admissionDate) => {
  const request = new CreateStudentRequest();
  const date = new Date();
  setDateInfoFromJSON(date, admissionDate);

  request.setUniversityassignedid(universityAssignedId);
  request.setFullname(fullName);
  request.setEmail(email);
  request.setAdmissiondate(date);

  const student = await studentsService.createStudent(request, { userId: REACT_APP_ISSUER });

  Logger.info('Created student:', student.toObject());
};
