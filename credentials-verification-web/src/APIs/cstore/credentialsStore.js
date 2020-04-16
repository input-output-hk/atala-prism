import { CredentialsStoreServicePromiseClient } from '../../protos/cstore_api_grpc_web_pb';
import Logger from '../../helpers/Logger';

const {
  GenerateConnectionTokenForRequest,
  GetIndividualsRequest,
  CreateIndividualRequest
} = require('../../../src/protos/cstore_api_pb');

async function getIndividuals(lastSeenId, limit = 10) {
  Logger.info(`Getting individuals limit ${limit}, lastSeenId ${lastSeenId}`);
  const getIndividualsRequest = new GetIndividualsRequest();
  getIndividualsRequest.setLimit(limit);
  if (lastSeenId) getIndividualsRequest.setLastseenindividualid(lastSeenId);

  const metadata = await this.auth.getMetadata(getIndividualsRequest);

  const response = await this.client.getIndividuals(getIndividualsRequest, metadata);
  const { individualsList } = response.toObject();

  return individualsList;
}

async function generateConnectionTokenForIndividual(individualId) {
  Logger.info(`Generating connection token for individualId ${individualId}`);
  const request = new GenerateConnectionTokenForRequest();
  request.setIndividualid(individualId);

  const metadata = await this.auth.getMetadata(request);

  const res = await this.client.generateConnectionTokenFor(request, metadata);

  return res.getToken();
}

async function createIndividual(fullName, email) {
  const request = new CreateIndividualRequest();

  request.setFullname(fullName);
  request.setEmail(email);

  const metadata = await this.auth.getMetadata(request);

  const individual = await this.client.createIndividual(request, metadata);

  return individual.toObject();
}

function CredentialsStore(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsStoreServicePromiseClient(this.config.grpcClient, null, null);
}

CredentialsStore.prototype.getIndividualsAsVerifier = getIndividuals;
CredentialsStore.prototype.generateConnectionTokenForIndividual = generateConnectionTokenForIndividual;
CredentialsStore.prototype.createIndividual = createIndividual;

export default CredentialsStore;
