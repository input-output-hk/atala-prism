import { CredentialsStoreServicePromiseClient } from '../../protos/cstore_api_grpc_web_pb';
import Logger from '../../helpers/Logger';

const {
  GenerateConnectionTokenForRequest,
  GetIndividualsRequest,
  CreateIndividualRequest
} = require('../../../src/protos/cstore_api_pb');

const { config } = require('../config');

const credentialsService = new CredentialsStoreServicePromiseClient(config.grpcClient, null, null);

export const getIndividuals = async (aUserId = config.verifierId, lastSeenId, limit = 10) => {
  const userId = aUserId || config.verifierId;
  Logger.info(`Getting individuals userId ${userId}, limit ${limit}, lastSeenId ${lastSeenId}`);
  const getIndividualsRequest = new GetIndividualsRequest();
  getIndividualsRequest.setLimit(limit);
  if (lastSeenId) getIndividualsRequest.setLastseenindividualid(lastSeenId);
  const response = await credentialsService.getIndividuals(getIndividualsRequest, { userId });
  const { individualsList } = response.toObject();

  return individualsList;
};

export const generateConnectionTokenForIndividual = async (
  aUserId = config.verifierId,
  individualId
) => {
  const userId = aUserId || config.verifierId;
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
    userId: config.verifierId
  });

  return individual.toObject();
};
