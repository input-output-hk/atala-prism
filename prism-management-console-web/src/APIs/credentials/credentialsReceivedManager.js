import base64url from 'base64url';
import { CredentialsStoreServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import { GetStoredCredentialsForRequest } from '../../protos/console_api_pb';
import Logger from '../../helpers/Logger';
import { BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } from '../../helpers/constants';
import { credentialReceivedMapper } from '../helpers/credentialHelpers';

/**
 *
 * @param {string} contactId
 * @param {Object} contactsManager required to handle additional request to fetch contact attributes
 * @returns {Array} credentials received
 */
async function getReceivedCredentials(contactId, contactsManager) {
  const contactMessage = contactId ? ` for contact ${contactId}` : '';
  Logger.info(`Getting received credentials${contactMessage}`);
  const req = new GetStoredCredentialsForRequest();
  if (contactId) req.setIndividualId(contactId);
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
    const parsedCredentialData = JSON.parse(decodedCredential);
    const credential = Object.assign(parsedCredentialData, rest);
    return Object.assign(credential, { encodedSignedCredential });
  });
  /**
   * Additional request to fetch contact's data.
   * TODO: update backend so it provides all the required fields
   */
  const credentialsWithContactsData = credentials.map(credential =>
    contactsManager
      .getContact(credential.individualId)
      .then(contactData => ({ contactData, ...credential }))
  );
  const credentialsWithIssuanceProof = await Promise.all(credentialsWithContactsData);

  const mappedCredentials = credentialsWithIssuanceProof.map(cred =>
    credentialReceivedMapper(cred)
  );

  Logger.info('Got received credentials:', mappedCredentials);
  return mappedCredentials;
}

function CredentialsReceivedManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsStoreServicePromiseClient(config.grpcClient, null, null);
}

CredentialsReceivedManager.prototype.getReceivedCredentials = getReceivedCredentials;

export default CredentialsReceivedManager;
