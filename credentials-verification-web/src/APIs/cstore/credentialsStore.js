import { CredentialsStoreServicePromiseClient } from '../../protos/cstore/cstore_grpc_web_pb';
import Logger from '../../helpers/Logger';

const {
  GenerateConnectionTokenForRequest,
  GetIndividualsRequest,
  CreateIndividualRequest,
  GetStoredCredentialsForRequest
} = require('../../../src/protos/cstore/cstore_pb');

const { REACT_APP_GRPC_CLIENT, REACT_APP_VERIFIER } = window._env_;
const credentialsService = new CredentialsStoreServicePromiseClient(
  REACT_APP_GRPC_CLIENT,
  null,
  null
);

export const getIndividuals = async (aUserId = REACT_APP_VERIFIER, limit = 10, lastSeenId) => {
  const userId = aUserId || REACT_APP_VERIFIER;
  Logger.info(`Getting individuals userId ${userId}, limit ${limit}, lastSeenId ${lastSeenId}`);
  const getIndividualsRequest = new GetIndividualsRequest();
  getIndividualsRequest.setLimit(limit);
  if (lastSeenId) getIndividualsRequest.setLastseenindividualid(lastSeenId);
  const response = await credentialsService.getIndividuals(getIndividualsRequest, { userId });
  const { individualsList } = response.toObject();

  return individualsList;
};

export const generateConnectionTokenForIndividual = async (
  aUserId = REACT_APP_VERIFIER,
  individualId
) => {
  const userId = aUserId || REACT_APP_VERIFIER;
  Logger.info(`Generating connection token for individualId ${individualId} with userId ${userId}`);
  const request = new GenerateConnectionTokenForRequest();
  request.setIndividualid(individualId);
  const res = await credentialsService.generateConnectionTokenFor(request, { userId });

  return res.getToken();
};

export const createIndividual = async (fullName, email) => {
  const request = new CreateIndividualRequest();

  request.setFullname(fullName);
  request.setEmail(email);

  const individual = await credentialsService.createIndividual(request, {
    userId: REACT_APP_VERIFIER
  });

  return individual.toObject();
};
