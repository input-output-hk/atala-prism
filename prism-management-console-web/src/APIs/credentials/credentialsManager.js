import { CredentialsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import credentialTypes from './credentialTypes';
import {
  REQUEST_AUTH_TIMEOUT_MS,
  FAILED,
  SUCCESS,
  CREDENTIAL_PAGE_SIZE
} from '../../helpers/constants';
import { getAditionalTimeout } from '../../helpers/genericHelpers';

const {
  GetGenericCredentialsRequest,
  CreateGenericCredentialRequest,
  GetContactCredentialsRequest,
  ShareCredentialRequest,
  GetBlockchainDataRequest
} = require('../../protos/console_api_pb');
const { AtalaMessage, PlainTextCredential } = require('../../protos/credential_models_pb');

function mapCredential(cred) {
  const credential = cred.toObject();
  const credentialData = JSON.parse(cred.getCredentialdata());
  const subjectData = JSON.parse(cred.getContactdata());
  return Object.assign(credential, { subjectData }, credentialData);
}

async function getCredentials(limit = CREDENTIAL_PAGE_SIZE, lastSeenCredentialId = null) {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);

  const getCredentialsRequest = new GetGenericCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  getCredentialsRequest.setLastseencredentialid(lastSeenCredentialId);

  const timeout = REQUEST_AUTH_TIMEOUT_MS + getAditionalTimeout(limit);

  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialsRequest, timeout);
  if (sessionError) return [];

  const result = await this.client.getGenericCredentials(getCredentialsRequest, metadata);

  const credentialsList = result.getCredentialsList().map(mapCredential);

  return credentialsList;
}

async function createBatchOfCredentials(credentialsData) {
  Logger.info(`Creating ${credentialsData?.length} credential(s):`);

  const credentialStudentsPromises = credentialsData.map(({ externalid, contactid, ...json }) => {
    const createCredentialRequest = new CreateGenericCredentialRequest();

    createCredentialRequest.setContactid(contactid);
    createCredentialRequest.setExternalid(externalid);
    createCredentialRequest.setCredentialdata(JSON.stringify(json));

    return this.auth
      .getMetadata(createCredentialRequest)
      .then(({ metadata }) =>
        this.client.createGenericCredential(createCredentialRequest, metadata)
      )
      .then(response => ({ externalid, status: SUCCESS, response }))
      .catch(error => {
        Logger.error(error);
        return { externalid, status: FAILED, error };
      });
  });

  return Promise.all(credentialStudentsPromises);
}

function getCredentialBinary(credential) {
  const { encodedsignedcredential, batchinclusionproof } = credential;
  if (!encodedsignedcredential) {
    Logger.error('Could not get encoded credential', credential);
    throw new Error('No encoded credential');
  }

  if (!batchinclusionproof) {
    Logger.error('Could not get batch inclusion proof', credential);
    throw new Error('No inclusion proof');
  }

  const atalaMessage = new AtalaMessage();
  const plainTextCredential = new PlainTextCredential();

  plainTextCredential.setEncodedcredential(encodedsignedcredential);
  plainTextCredential.setEncodedmerkleproof(batchinclusionproof);

  atalaMessage.setPlaincredential(plainTextCredential);
  return atalaMessage.serializeBinary();
}

function getCredentialTypes() {
  return credentialTypes;
}

async function getContactCredentials(contactId) {
  Logger.info('Getting credentials for contact:', contactId);
  const req = new GetContactCredentialsRequest();
  req.setContactid(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return [];

  const res = await this.client.getContactCredentials(req, metadata);
  const credentialsList = res.getGenericcredentialsList().map(mapCredential);
  Logger.info('Got credentials:', credentialsList);

  return credentialsList;
}

async function markAsSent(credentialid) {
  const markCredentialRequest = new ShareCredentialRequest();
  markCredentialRequest.setCmanagercredentialid(credentialid);

  const { metadata, sessionError } = await this.auth.getMetadata(
    markCredentialRequest,
    REQUEST_AUTH_TIMEOUT_MS
  );
  if (sessionError) return;

  const res = await this.client.shareCredential(markCredentialRequest, metadata);
  Logger.info(`Marked credential (${credentialid}) as sent`);
  return res;
}

async function getBlockchainData(credential) {
  const getBlockchainDataRequest = new GetBlockchainDataRequest();
  getBlockchainDataRequest.setEncodedsignedcredential(credential);

  const { metadata, sessionError } = await this.auth.getMetadata(getBlockchainDataRequest);
  if (sessionError) return {};

  const res = await this.client.getBlockchainData(getBlockchainDataRequest, metadata);
  const issuanceProof = res.getIssuanceproof()?.toObject();
  Logger.info('Got issuance proof:', issuanceProof);
  return issuanceProof;
}

function CredentialsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsServicePromiseClient(config.grpcClient, null, null);
}

CredentialsManager.prototype.getCredentials = getCredentials;
CredentialsManager.prototype.createBatchOfCredentials = createBatchOfCredentials;
CredentialsManager.prototype.getCredentialBinary = getCredentialBinary;
CredentialsManager.prototype.getCredentialTypes = getCredentialTypes;
CredentialsManager.prototype.getContactCredentials = getContactCredentials;
CredentialsManager.prototype.markAsSent = markAsSent;
CredentialsManager.prototype.getBlockchainData = getBlockchainData;

export default CredentialsManager;
