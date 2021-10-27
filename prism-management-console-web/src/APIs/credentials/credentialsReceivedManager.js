import base64url from 'base64url';
import { CredentialsStoreServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import { GetStoredCredentialsForRequest } from '../../protos/console_api_pb';
import Logger from '../../helpers/Logger';
import { BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } from '../../helpers/constants';

async function getReceivedCredentials(contactId) {
  const contactMessage = contactId ? ` for contact ${contactId}` : '';
  Logger.info(`Getting received credentials${contactMessage}`);
  const req = new GetStoredCredentialsForRequest();
  if (contactId) req.setIndividualid(contactId);
  const { metadata, sessionError } = await this.auth.getMetadata(
    req,
    BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS
  );
  if (sessionError) return [];

  const res = await this.client.getStoredCredentialsFor(req, metadata);
  const credentials = res.getCredentialsList().map(storedCredential => {
    const { encodedSignedCredential, ...rest } = storedCredential.toObject();
    const [encodedCredential] = encodedSignedCredential.split('.');
    const decodedCredential = base64url.decode(encodedCredential);
    const credentialData = JSON.parse(decodedCredential);
    const credential = Object.assign(credentialData, rest);
    return credential;
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
