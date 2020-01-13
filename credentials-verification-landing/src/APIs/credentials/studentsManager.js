/* eslint import/no-unresolved: 0 */ // --> OFF
import { StudentsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import {
  GetStudentRequest,
  GenerateConnectionTokenRequest,
  CreateStudentRequest,
  Date
} from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';
import { LANDING_UNIVERSITY } from '../../helpers/constants';
import { setDateInfoFromJSON } from '../helpers';

const { REACT_APP_GRPC_CLIENT, REACT_APP_ISSUER_ID } = window._env_;
const issuerId = REACT_APP_ISSUER_ID;
const studentsService = new StudentsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

export const generateConnectionToken = async (userId, studentId) => {
  const hardCodedUserId = REACT_APP_ISSUER_ID;
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

export const createStudent = async ({ fullName, email, admissionDate }) => {
  const request = new CreateStudentRequest();
  const date = new Date();

  setDateInfoFromJSON(date, admissionDate);

  request.setUniversityassignedid(LANDING_UNIVERSITY);
  request.setFullname(fullName);
  request.setEmail(email);
  request.setAdmissiondate(date);

  const response = await studentsService.createStudent(request, { userId: REACT_APP_ISSUER_ID });

  const { student } = response.toObject();

  return student;
};
