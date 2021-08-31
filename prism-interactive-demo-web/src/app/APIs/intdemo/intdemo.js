import {
  IDServicePromiseClient,
  DegreeServicePromiseClient,
  EmploymentServicePromiseClient,
  InsuranceServicePromiseClient
} from '../../protos/intdemo/intdemo_api_grpc_web_pb';
import { config } from '../configs';
import {
  GOVERNMENT_ISSUED_DIGITAL_IDENTITY,
  UNIVERSITY_DEGREE,
  PROOF_OF_EMPLOYMENT,
  INSURANCE_POLICY
} from '../../helpers/constants';
import Logger from '../../helpers/Logger';

const {
  GetConnectionTokenRequest,
  GetSubjectStatusRequest,
  SetPersonalDataRequest
} = require('../../protos/intdemo/intdemo_api_pb');

const { Date } = require('../../protos/intdemo/intdemo_models_pb');

const idService = new IDServicePromiseClient(config.grpcClient, null, null);
const degreeService = new DegreeServicePromiseClient(config.grpcClient, null, null);
const insuranceService = new EmploymentServicePromiseClient(config.grpcClient, null, null);
const employmentService = new InsuranceServicePromiseClient(config.grpcClient, null, null);

const getUserId = () => config.issuerId;

const ServiceByCredential = {
  [GOVERNMENT_ISSUED_DIGITAL_IDENTITY]: idService,
  [UNIVERSITY_DEGREE]: degreeService,
  [PROOF_OF_EMPLOYMENT]: insuranceService,
  [INSURANCE_POLICY]: employmentService
};

export const getConnectionToken = async currentCredential => {
  Logger.info('getting connection token');
  const request = new GetConnectionTokenRequest();
  const result = await ServiceByCredential[currentCredential].getConnectionToken(request, {
    userId: getUserId()
  });
  return result.getConnectiontoken();
};

export const startSubjectStatusStream = (
  currentCredential,
  connectionToken,
  onData,
  handleError,
  onClosed
) => {
  Logger.info('Getting subject status', connectionToken);
  const request = new GetSubjectStatusRequest();
  request.setConnectiontoken(connectionToken);
  const stream = ServiceByCredential[currentCredential].getSubjectStatusStream(request, {
    userId: getUserId()
  });
  stream.on('data', response => {
    Logger.info('stream data:', response);
    Logger.info(response.getSubjectstatus());
    onData(response.getSubjectstatus());
  });
  stream.on('end', end => {
    Logger.info('ending stream:', end);
    if (onClosed) onClosed(end);
  });
  stream.on('error', error => {
    Logger.info('error on stream:', error);
    if (handleError) handleError(error);
    if (onClosed) onClosed(error);
  });
};

export const setPersonalData = data => {
  const { connectionToken, firstName, dateOfBirth } = data;
  Logger.info('setting personal data', data);
  const date = new Date();
  date.setYear(dateOfBirth.year);
  date.setMonth(dateOfBirth.month);
  date.setDay(dateOfBirth.day);
  const request = new SetPersonalDataRequest();
  request.setConnectiontoken(connectionToken);
  request.setFirstname(firstName);
  request.setDateofbirth(date);
  idService.setPersonalData(request, { userId: getUserId() });
};
