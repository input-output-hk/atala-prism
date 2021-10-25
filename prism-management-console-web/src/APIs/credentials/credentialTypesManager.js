import { CredentialTypesServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { GetCredentialTypesRequest, GetCredentialTypeRequest } from '../../protos/console_api_pb';
import { adaptCredentialType } from '../helpers/credentialTypeHelpers';
import hardcodedTemplateCategories from './mocks/hardcodedTemplateCategories';
import { VALIDATION_KEYS } from '../../helpers/constants';

async function getCredentialTypes() {
  Logger.info('getting credential types');
  const getCredentialTypesRequest = new GetCredentialTypesRequest();

  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialTypesRequest);
  if (sessionError) return [];

  const response = await this.client.getCredentialTypes(getCredentialTypesRequest, metadata);

  const { credentialTypesList } = response.toObject();
  const adaptedCredentialTypesList = credentialTypesList.map(adaptCredentialType);
  Logger.info('got credential types: ', adaptedCredentialTypesList);

  return adaptedCredentialTypesList;
}

async function getCredentialTypeDetails(id) {
  const getCredentialTypeDetailsRequest = new GetCredentialTypeRequest();
  getCredentialTypeDetailsRequest.setCredentialTypeId(id);
  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialTypeDetailsRequest);
  if (sessionError) return [];

  const response = await this.client.getCredentialType(getCredentialTypeDetailsRequest, metadata);

  const { credentialType } = response.toObject();
  const mappedCredentialType = {
    ...adaptCredentialType(credentialType.credentialType),
    fields: credentialType.requiredFieldsList.map(mapCredentialTypeField)
  };

  Logger.info('got credential details: ', mappedCredentialType);
  return mappedCredentialType;
}

// eslint-disable-next-line no-unused-vars
async function createTemplate(_values) {
  // TODO: add implementation for creating credential types
  return Promise.resolve();
}

async function getTemplateCategories() {
  return Promise.resolve(hardcodedTemplateCategories);
}

// eslint-disable-next-line no-unused-vars
async function createCategory(values) {
  return Promise.resolve(values);
}

const mapCredentialTypeField = field => ({
  ...field,
  key: field.name,
  validations: [VALIDATION_KEYS.REQUIRED]
});

function CredentialTypesManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialTypesServicePromiseClient(config.grpcClient, null, null);
}

CredentialTypesManager.prototype.getCredentialTypes = getCredentialTypes;
CredentialTypesManager.prototype.getCredentialTypeDetails = getCredentialTypeDetails;
CredentialTypesManager.prototype.createTemplate = createTemplate;
CredentialTypesManager.prototype.getTemplateCategories = getTemplateCategories;
CredentialTypesManager.prototype.createCategory = createCategory;

export default CredentialTypesManager;
