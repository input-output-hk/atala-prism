import { CredentialsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import { GetCredentialsRequest } from '../../protos/credentials/credentialsManager_pb';
import Logger from '../../helpers/Logger';

const { REACT_APP_GRPC_CLIENT } = process.env;
const issuerId = 'c8834532-eade-11e9-a88d-d8f2ca059830';
const credentialsService = new CredentialsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

export const getCredentials = async (limit = 10, lastSeenCredentialId = null) => {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);
  const getCredentialsRequest = new GetCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  const result = await credentialsService.getCredentials(getCredentialsRequest, {
    userId: issuerId
  });
  const { credentialsList } = result.toObject();
  return { credentials: credentialsList, count: credentialsList.length };
};
