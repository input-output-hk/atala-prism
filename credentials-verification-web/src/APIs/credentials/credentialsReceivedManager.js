import base64url from 'base64url';
import { CredentialsStoreServicePromiseClient } from '../../protos/cstore_api_grpc_web_pb';
import { GetStoredCredentialsForRequest } from '../../protos/cstore_api_pb';
import Logger from '../../helpers/Logger';

async function getReceivedCredentials(contactId) {
  Logger.info(`Getting received credentials ${contactId ? `for contact ${contactId}` : ''}`);
  const req = new GetStoredCredentialsForRequest();
  if (contactId) req.setIndividualid(contactId);
  const metadata = await this.auth.getMetadata(req);
  const res = await this.client.getStoredCredentialsFor(req, metadata);
  const credentials = res.getCredentialsList().map(storedCredential => {
    const encodedsignedcredential = storedCredential.getEncodedsignedcredential();
    const [encodedCredential] = encodedsignedcredential.split('.');
    const decodedCredential = base64url.decode(encodedCredential);
    const { claims: credential } = JSON.parse(decodedCredential);
    return Object.assign({ encodedsignedcredential }, credential);
  });
  Logger.info('Got received credentials:', credentials);
  return credentials;
}

function CredentialsReceivedManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsStoreServicePromiseClient(config.grpcClient, null, null);
}

CredentialsReceivedManager.prototype.getReceivedCredentials = getReceivedCredentials;

export default CredentialsReceivedManager;
