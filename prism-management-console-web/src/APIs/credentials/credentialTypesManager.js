import _ from 'lodash';
import { CredentialTypesServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import {
  GetCredentialTypesRequest,
  GetCredentialTypeRequest,
  CreateCredentialTypeRequest
} from '../../protos/console_api_pb';
import { CreateCredentialType, CreateCredentialTypeField } from '../../protos/console_models_pb';
import { adaptCredentialType } from '../helpers/credentialTypeHelpers';
import { VALIDATION_KEYS } from '../../helpers/constants';

async function getCredentialTypes() {
  Logger.info('getting credential types');
  const getCredentialTypesRequest = new GetCredentialTypesRequest();

  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialTypesRequest);
  if (sessionError) return [];

  const response = await this.client.getCredentialTypes(getCredentialTypesRequest, metadata);

  const { credentialTypesList } = response.toObject();
  const adaptedCredentialTypesList = credentialTypesList.map(adaptCredentialType);

  /**
   * This additional mapping injects the credential types with the corresponding category,
   * stored in the local storage (for now)
   * TODO: remove when backend supports categories
   */
  const customCredentialTypesCategories = this.config.getCredentialTypesWithCategories();
  const adaptedCredentialTypesListWithMockedCategories = adaptedCredentialTypesList.map(c => ({
    ...c,
    category: customCredentialTypesCategories[c.id] || c.category
  }));
  Logger.info('got credential types: ', adaptedCredentialTypesListWithMockedCategories);

  return adaptedCredentialTypesListWithMockedCategories;
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

async function createCredentialType(values) {
  const createCredentialTypeRequest = new CreateCredentialTypeRequest();
  const credentialTypeModel = new CreateCredentialType();

  const [_prefix, iconData] = values.icon.split(',');

  credentialTypeModel.setName(values.name);
  credentialTypeModel.setTemplate(values.template);
  credentialTypeModel.setIcon(iconData);

  const fields = values.credentialBody.map(fieldData => {
    const fieldObj = new CreateCredentialTypeField();
    fieldObj.setName(_.camelCase(fieldData.attributeLabel));
    fieldObj.setDescription('');
    fieldObj.setType(fieldData.attributeType);
    return fieldObj;
  });

  credentialTypeModel.setFieldsList(fields);
  createCredentialTypeRequest.setCredentialType(credentialTypeModel);

  const { metadata, sessionError } = await this.auth.getMetadata(createCredentialTypeRequest);
  if (sessionError) return [];

  const response = await this.client.createCredentialType(createCredentialTypeRequest, metadata);

  const responseObject = response.toObject();
  const {
    credentialType: { credentialType }
  } = responseObject;
  /**
   * Here we persist the credential type -- category relationship in the local storage
   * TODO: remove when backend supports categories
   */
  this.config.saveCredentialTypeWithCategory({
    credentialTypeId: credentialType.id,
    category: values.category
  });

  return credentialType;
}

function getTemplateCategories() {
  return this.config.getMockedTemplateCategories();
}

function createCategory(values) {
  const currentTemplateCategories = this.getTemplateCategories();
  const updatedCategories = currentTemplateCategories.concat(values);
  this.config.saveMockedTemplateCategories(updatedCategories);
  return values;
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
CredentialTypesManager.prototype.createCredentialType = createCredentialType;
CredentialTypesManager.prototype.getTemplateCategories = getTemplateCategories;
CredentialTypesManager.prototype.createCategory = createCategory;

export default CredentialTypesManager;
