import { CredentialsStoreServicePromiseClient } from '../../protos/cstore_api_grpc_web_pb';
import { holderToIndividual } from '../helpers';
import Logger from '../../helpers/Logger';

const {
  GenerateConnectionTokenForRequest,
  GetHoldersRequest,
  CreateHolderRequest
} = require('../../../src/protos/cstore_api_pb');

async function getHolders(lastSeenId, limit = 10) {
  Logger.info(`Getting holders limit ${limit}, lastSeenId ${lastSeenId}`);
  const getHoldersRequest = new GetHoldersRequest();
  getHoldersRequest.setLimit(limit);
  if (lastSeenId) getHoldersRequest.setLastseenholderid(lastSeenId);

  const metadata = await this.auth.getMetadata(getHoldersRequest);

  const response = await this.client.getHolders(getHoldersRequest, metadata);
  const { holdersList } = response.toObject();

  return holdersList.map(holderToIndividual);
}

async function generateConnectionTokenForHolder(holderId) {
  Logger.info(`Generating connection token for holderId ${holderId}`);
  const request = new GenerateConnectionTokenForRequest();
  // TODO: rename to holderid when teh API changes
  request.setIndividualid(holderId);

  const metadata = await this.auth.getMetadata(request);

  const res = await this.client.generateConnectionTokenFor(request, metadata);

  return res.getToken();
}

async function createHolder(fullname, email) {
  const request = new CreateHolderRequest();

  request.setJsondata(JSON.stringify({ fullname, email }));

  const metadata = await this.auth.getMetadata(request);

  const { holder } = (await this.client.createHolder(request, metadata)).toObject();

  return holderToIndividual(holder);
}

function CredentialsStore(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsStoreServicePromiseClient(this.config.grpcClient, null, null);
}

CredentialsStore.prototype.getHoldersAsVerifier = getHolders;
CredentialsStore.prototype.generateConnectionTokenForHolder = generateConnectionTokenForHolder;
CredentialsStore.prototype.createHolder = createHolder;

export default CredentialsStore;
