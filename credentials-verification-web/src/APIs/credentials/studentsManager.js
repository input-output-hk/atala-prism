import { StudentsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import { GetStudentsRequest } from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = process.env;
const issuerId = 'c8834532-eade-11e9-a88d-d8f2ca059830';
const studentsService = new StudentsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

const createAndPopulateGetStudentRequest = (limit, lastSeenStudentId) => {
  const getStudentsRequest = new GetStudentsRequest();

  getStudentsRequest.setLimit(limit);
  getStudentsRequest.setLastseenstudentid(lastSeenStudentId);

  return getStudentsRequest;
};

export const getStudents = async (limit = 10, lastSeenCredentialId = null) => {
  Logger.info('Getting the students');
  const getStudentsRequest = createAndPopulateGetStudentRequest(limit, lastSeenCredentialId);

  const result = await studentsService.getStudents(getStudentsRequest, { userId: issuerId });

  const { studentsList } = result.toObject();

  return studentsList;
};
