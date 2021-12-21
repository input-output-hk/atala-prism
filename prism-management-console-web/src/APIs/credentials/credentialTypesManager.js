import _ from 'lodash';
import {
  CredentialTypesServicePromiseClient,
  CredentialTypeCategoriesServicePromiseClient
} from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import {
  GetCredentialTypesRequest,
  GetCredentialTypeRequest,
  CreateCredentialTypeRequest,
  GetCredentialTypeCategoriesRequest,
  CreateCredentialTypeCategoryRequest,
  MarkAsReadyCredentialTypeRequest,
  UpdateCredentialTypeRequest
} from '../../protos/console_api_pb';
import {
  CreateCredentialType,
  CreateCredentialTypeField,
  CreateCredentialTypeCategory,
  UpdateCredentialType
} from '../../protos/console_models_pb';
import { CREDENTIAL_TYPE_CATEGORY_STATUSES, VALIDATION_KEYS } from '../../helpers/constants';
import governmentIdLogo from '../../images/government-id-logo.svg';
import educationalLogo from '../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../images/health-insurance-logo.svg';
import { svgPathToEncodedBase64 } from '../../helpers/genericHelpers';
import { b64ImagePrefix } from '../helpers/credentialTypeHelpers';

const { READY } = CREDENTIAL_TYPE_CATEGORY_STATUSES;

const defaultTemplatesAttributesMapping = {
  id: {
    name: 'Government ID',
    categoryName: 'Identity',
    iconPath: governmentIdLogo
  },
  educational: {
    name: 'Educational',
    categoryName: 'Educational',
    iconPath: educationalLogo
  },
  health: {
    name: 'Health Insurance',
    categoryName: 'Health',
    iconPath: healthInsuranceLogo
  },
  employment: {
    name: 'Proof Of Employment',
    categoryName: 'Employment',
    iconPath: proofOfEmploymentLogo
  }
};

const defaultCategoryNames = Object.values(defaultTemplatesAttributesMapping).map(
  t => t.categoryName
);

async function getIconData(credentialTypeName) {
  const { iconPath } = defaultTemplatesAttributesMapping[credentialTypeName] || {};
  const encodedIcon = await svgPathToEncodedBase64(iconPath);
  const [_prefix, iconData] = encodedIcon.split(',');
  return iconData;
}

async function getCredentialTypes() {
  Logger.info('getting credential types');
  const getCredentialTypesRequest = new GetCredentialTypesRequest();

  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialTypesRequest);
  if (sessionError) return [];

  const response = await this.credentialTypesServiceClient.getCredentialTypes(
    getCredentialTypesRequest,
    metadata
  );

  const { credentialTypesList } = response.toObject();

  // TODO: remove when backend provides complete default credential types
  const hasBeenUpdated = await this.completeDefaultCredentialTypes(credentialTypesList);

  if (hasBeenUpdated) return this.getCredentialTypes();

  /**
   * This additional mapping injects the credential types with the corresponding category,
   * stored in the local storage (for now)
   * TODO: remove when backend supports categories
   */
  const categoriesAssociatedWithCredentialTypes = this.config.getCredentialTypesWithCategories();
  const credentialTypesWithCategories = credentialTypesList.map(c => ({
    ...c,
    icon: `${b64ImagePrefix},${c.icon}`,
    category: categoriesAssociatedWithCredentialTypes[c.id]
  }));
  Logger.info('got credential types: ', credentialTypesWithCategories);

  return credentialTypesWithCategories;
}

async function getCredentialTypeDetails(id) {
  const getCredentialTypeDetailsRequest = new GetCredentialTypeRequest();
  getCredentialTypeDetailsRequest.setCredentialTypeId(id);
  const { metadata, sessionError } = await this.auth.getMetadata(getCredentialTypeDetailsRequest);
  if (sessionError) return [];

  const response = await this.credentialTypesServiceClient.getCredentialType(
    getCredentialTypeDetailsRequest,
    metadata
  );

  const { credentialType } = response.toObject();
  const mappedCredentialType = {
    ...credentialType.credentialType,
    icon: `${b64ImagePrefix},${credentialType.credentialType.icon}`,
    fields: credentialType.requiredFieldsList.map(mapCredentialTypeField)
  };

  Logger.info('got credential details: ', mappedCredentialType);
  return mappedCredentialType;
}

const mapCredentialTypeField = field => ({
  ...field,
  key: field.name,
  validations: [VALIDATION_KEYS.REQUIRED]
});

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

  const response = await this.credentialTypesServiceClient.createCredentialType(
    createCredentialTypeRequest,
    metadata
  );

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

async function updateCredentialType(credentialTypeId, credentialTypeData, categoryId) {
  const updateCredentialTypeRequest = new UpdateCredentialTypeRequest();
  const credentialTypeModel = new UpdateCredentialType();

  credentialTypeModel.setId(credentialTypeId);
  if (credentialTypeData.name) credentialTypeModel.setName(credentialTypeData.name);
  if (credentialTypeData.icon) credentialTypeModel.setIcon(credentialTypeData.icon);

  // Prevent resetting template. TODO: passing (null | undefined) should leave template unchanged
  if (credentialTypeData.template) credentialTypeModel.setTemplate(credentialTypeData.template);

  const credentialTypeDetails = await this.getCredentialTypeDetails(credentialTypeData.id);
  // Prevent resetting fields. TODO: passing (null | undefined) should leave fields unchanged
  const fields = credentialTypeDetails.fields.map(fieldData => {
    const fieldObj = new CreateCredentialTypeField();
    fieldObj.setName(fieldData.name);
    fieldObj.setDescription(fieldData.description);
    fieldObj.setType(fieldData.type);
    return fieldObj;
  });

  credentialTypeModel.setFieldsList(fields);

  updateCredentialTypeRequest.setCredentialType(credentialTypeModel);

  const { metadata, sessionError } = await this.auth.getMetadata(updateCredentialTypeRequest);
  if (sessionError) return [];

  const response = await this.credentialTypesServiceClient.updateCredentialType(
    updateCredentialTypeRequest,
    metadata
  );

  if (categoryId) {
    this.config.saveCredentialTypeWithCategory({
      credentialTypeId,
      category: categoryId
    });
  }

  return response.toObject();
}

async function markCredentialTypeAsReady(template) {
  const markAsReadyCredentialTypeRequest = new MarkAsReadyCredentialTypeRequest();

  markAsReadyCredentialTypeRequest.setCredentialTypeId(template.id);

  const { metadata, sessionError } = await this.auth.getMetadata(markAsReadyCredentialTypeRequest);
  if (sessionError) return [];

  const response = await this.credentialTypesServiceClient.markAsReadyCredentialType(
    markAsReadyCredentialTypeRequest,
    metadata
  );

  /**
   * Here we persist the credential type -- category relationship in the local storage
   * TODO: remove when backend supports categories
   */

  return response.toObject();
}

async function getTemplateCategories() {
  const getCredentialTypeCategoriesRequest = new GetCredentialTypeCategoriesRequest();

  const { metadata, sessionError } = await this.auth.getMetadata(
    getCredentialTypeCategoriesRequest
  );

  if (sessionError) return [];
  const response = await this.categoriesServiceClient.getCredentialTypeCategories(
    getCredentialTypeCategoriesRequest,
    metadata
  );
  const { credentialTypeCategoriesList } = response.toObject();

  // TODO: remove when backend provides default categories from the start
  const createdCategories = await this.createMissingDefaultCategories(credentialTypeCategoriesList);

  return credentialTypeCategoriesList.concat(createdCategories);
}

async function createCategory(values) {
  const createCredentialTypeCategoryRequest = new CreateCredentialTypeCategoryRequest();

  const credentialTypeCategoryModel = new CreateCredentialTypeCategory();
  credentialTypeCategoryModel.setName(values.name);
  credentialTypeCategoryModel.setState(values.state);

  createCredentialTypeCategoryRequest.setCredentialTypeCategory(credentialTypeCategoryModel);

  const { metadata, sessionError } = await this.auth.getMetadata(
    createCredentialTypeCategoryRequest
  );
  if (sessionError) return [];

  const response = await this.categoriesServiceClient.createCredentialTypeCategory(
    createCredentialTypeCategoryRequest,
    metadata
  );

  const { credentialTypeCategory } = response.toObject();

  return credentialTypeCategory;
}

/**
 * Currently the backend provides incomplete default credential types.
 * This function handles the required modifications (only the first time).
 *
 * TODO: remove when backend provides complete credential types from the start
 *
 * @param {Array} credentialTypesList latest fetched credential types
 * @returns {Boolean} wether the credential types were updated or not
 */
async function completeDefaultCredentialTypes(credentialTypesList) {
  const typesToUpdate = credentialTypesList.filter(credentialType =>
    Object.keys(defaultTemplatesAttributesMapping).includes(credentialType.name)
  );

  // checks if already completed
  if (!typesToUpdate.length) return false;

  const categories = await this.getTemplateCategories();

  const getCategoryId = categoryName => categories.find(c => c.name === categoryName)?.id;

  const nameUpdatesPromisesArray = typesToUpdate.map(async credentialType => {
    const { name, categoryName } = defaultTemplatesAttributesMapping[credentialType.name];
    const categoryToAssociateWith = getCategoryId(categoryName);
    const credentialTypeData = {
      ...credentialType,
      name,
      icon: await getIconData(credentialType.name)
    };

    return this.updateCredentialType(
      credentialType.id,
      credentialTypeData,
      categoryToAssociateWith
    );
  });

  const nameUpdates = await Promise.all(nameUpdatesPromisesArray);

  const setAsReadyPromisesArray = typesToUpdate.map(credentialType =>
    this.markCredentialTypeAsReady(credentialType)
  );

  const stateUpdates = await Promise.all(setAsReadyPromisesArray);

  // returns truthy if any updates were made
  return Boolean(nameUpdates.length || stateUpdates.length);
}

/**
 * Currently the backend doesn't provide default categories.
 * This function handles the required creations (only the first time).
 *
 * TODO: remove when backend provides default categories from the start
 *
 * @param {Array} credentialTypeCategoriesList latest fetched categories
 * @returns {Array} created catefories
 */
async function createMissingDefaultCategories(credentialTypeCategoriesList) {
  const existingCategoryNames = credentialTypeCategoriesList.map(c => c.name);
  const missingDefaultCategories = defaultCategoryNames.filter(
    defaultCategoryName => !existingCategoryNames.includes(defaultCategoryName)
  );

  // checks if already completed
  if (!missingDefaultCategories.length) return [];

  const promisesArray = missingDefaultCategories.map(categoryName =>
    this.createCategory({ name: categoryName, state: READY })
  );

  return Promise.all(promisesArray);
}

function CredentialTypesManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.credentialTypesServiceClient = new CredentialTypesServicePromiseClient(
    config.grpcClient,
    null,
    null
  );
  this.categoriesServiceClient = new CredentialTypeCategoriesServicePromiseClient(
    config.grpcClient,
    null,
    null
  );
}

CredentialTypesManager.prototype.getCredentialTypes = getCredentialTypes;
CredentialTypesManager.prototype.getCredentialTypeDetails = getCredentialTypeDetails;
CredentialTypesManager.prototype.createCredentialType = createCredentialType;
CredentialTypesManager.prototype.getTemplateCategories = getTemplateCategories;
CredentialTypesManager.prototype.createCategory = createCategory;
CredentialTypesManager.prototype.updateCredentialType = updateCredentialType;
CredentialTypesManager.prototype.createMissingDefaultCategories = createMissingDefaultCategories;
CredentialTypesManager.prototype.completeDefaultCredentialTypes = completeDefaultCredentialTypes;
CredentialTypesManager.prototype.markCredentialTypeAsReady = markCredentialTypeAsReady;

export default CredentialTypesManager;
