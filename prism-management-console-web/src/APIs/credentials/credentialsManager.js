import _ from 'lodash';
import {
  CredentialsServicePromiseClient,
  CredentialIssuanceServicePromiseClient
} from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import hardcodedCredentialTypes from './mocks/hardcodedCredentialTypes';
import { REQUEST_AUTH_TIMEOUT_MS, CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';
import { getAditionalTimeout } from '../../helpers/genericHelpers';
import {
  GetGenericCredentialsRequest,
  GetContactCredentialsRequest,
  ShareCredentialRequest,
  GetBlockchainDataRequest,
  CreateGenericCredentialBulkRequest
} from '../../protos/console_api_pb';
import { AtalaMessage, PlainTextCredential } from '../../protos/credential_models_pb';
import { adaptCredentialType } from '../helpers/credentialTypeHelpers';

function mapCredential(cred) {
  const credential = cred.toObject();
  const credentialData = JSON.parse(cred.getCredentialData());
  const subjectData = JSON.parse(cred.getContactData());
  return Object.assign(credential, { subjectData }, credentialData);
}

async function getCredentials(limit = CREDENTIAL_PAGE_SIZE, lastSeenCredentialId = null) {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);

  const getCredentialsRequest = new GetGenericCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  getCredentialsRequest.setOffset(lastSeenCredentialId);

  const timeout = REQUEST_AUTH_TIMEOUT_MS + getAditionalTimeout(limit);

  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialsRequest, timeout);
  if (sessionError) return [];

  const result = await this.client.getGenericCredentials(getCredentialsRequest, metadata);

  const credentialsList = result.getCredentialsList().map(mapCredential);
  const adaptedCredentialsList = credentialsList.map(adaptCredential);
  Logger.info('Got credentials:', adaptedCredentialsList);

  return adaptedCredentialsList;
}

const adaptCredential = ({ credentialTypeDetails, ...rest }) => ({
  ...rest,
  credentialType: adaptCredentialType(credentialTypeDetails)
});

async function createBatchOfCredentials(credentialsData, credentialType, groups) {
  Logger.info(`Creating ${credentialsData.length} credential(s):`);
  const draftsToSend = credentialsData.map(({ policyNumber, ...c }) => ({
    external_id: c.externalId,
    // policyNumber is rejected as a string by the BE
    credential_data: _.omit({ ...c, policyNumber: parseInt(policyNumber, 10) }, [
      'externalId',
      'issuer',
      'credentialType'
    ]),
    group_ids: groups.map(g => g.id)
  }));
  const jsonToSend = {
    // FIXME: issuance_name is required to be unique by the backend.
    // Remove random value when it's no longer required.
    issuance_name: Math.random().toString(),
    credential_type_id: credentialType.id,
    drafts: draftsToSend
  };

  const req = new CreateGenericCredentialBulkRequest();
  req.setCredentialsJson(JSON.stringify(jsonToSend));

  const { metadata } = await this.auth.getMetadata(req);
  const res = await this.issuanceClient.createGenericCredentialBulk(req, metadata);

  return res.toObject();
}

function getCredentialBinary(credential) {
  const atalaMessage = generateAtalaMessage(credential);
  return atalaMessage.serializeBinary();
}

function generateAtalaMessage(credential) {
  const { encodedSignedCredential, batchInclusionProof } = credential;
  if (!encodedSignedCredential) {
    Logger.error('Could not get encoded credential', credential);
    throw new Error('No encoded credential');
  }

  if (!batchInclusionProof) {
    Logger.error('Could not get batch inclusion proof', credential);
    throw new Error('No inclusion proof');
  }

  const atalaMessage = new AtalaMessage();
  const plainTextCredential = new PlainTextCredential();

  plainTextCredential.setEncodedCredential(encodedSignedCredential);
  plainTextCredential.setEncodedMerkleProof(batchInclusionProof);

  atalaMessage.setPlainCredential(plainTextCredential);
  return atalaMessage;
}

function getCredentialTypes() {
  return hardcodedCredentialTypes;
}

async function getContactCredentials(contactId) {
  Logger.info('Getting credentials for contact:', contactId);
  const req = new GetContactCredentialsRequest();
  req.setContactId(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return [];

  const res = await this.client.getContactCredentials(req, metadata);
  const credentialsList = res.getGenericCredentialsList().map(mapCredential);
  Logger.info('Got credentials:', credentialsList);

  return credentialsList;
}

async function markAsSent(credentialId) {
  const markCredentialRequest = new ShareCredentialRequest();
  markCredentialRequest.setCmanagerCredentialId(credentialId);

  const { metadata, sessionError } = await this.auth.getMetadata(
    markCredentialRequest,
    REQUEST_AUTH_TIMEOUT_MS
  );
  if (sessionError) return;

  const res = await this.client.shareCredential(markCredentialRequest, metadata);
  Logger.info(`Marked credential (${credentialId}) as sent`);
  return res;
}

async function getBlockchainData(credential) {
  const getBlockchainDataRequest = new GetBlockchainDataRequest();
  getBlockchainDataRequest.setEncodedSignedCredential(credential);

  const { metadata, sessionError } = await this.auth.getMetadata(getBlockchainDataRequest);
  if (sessionError) return {};

  const res = await this.client.getBlockchainData(getBlockchainDataRequest, metadata);
  const issuanceProof = res.getIssuanceProof()?.toObject();
  Logger.info('Got issuance proof:', issuanceProof);
  return issuanceProof;
}

function CredentialsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsServicePromiseClient(config.grpcClient, null, null);
  this.issuanceClient = new CredentialIssuanceServicePromiseClient(config.grpcClient, null, null);
}

CredentialsManager.prototype.getCredentials = getCredentials;
CredentialsManager.prototype.createBatchOfCredentials = createBatchOfCredentials;
CredentialsManager.prototype.getCredentialBinary = getCredentialBinary;
CredentialsManager.prototype.generateAtalaMessage = generateAtalaMessage;
CredentialsManager.prototype.getCredentialTypes = getCredentialTypes;
CredentialsManager.prototype.getContactCredentials = getContactCredentials;
CredentialsManager.prototype.markAsSent = markAsSent;
CredentialsManager.prototype.getBlockchainData = getBlockchainData;

export default CredentialsManager;
