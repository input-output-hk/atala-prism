import { makeAutoObservable, flow } from 'mobx';
import { v4 as uuidv4 } from 'uuid';

export default class TemplateStore {
  isLoadingTemplates = false;

  isLoadingCategories = false;

  credentialTemplates = [];

  templateCategories = [];

  mockedCredentialTemplates = [];

  mockedTemplateCategories = [];

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.transportLayerErrorHandler = rootStore.sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;
    makeAutoObservable(this, {
      fetchTemplates: flow.bound,
      getCredentialTemplateDetails: flow.bound,
      createCredentialTemplate: flow.bound,
      fetchCategories: flow.bound,
      createTemplateCategory: flow.bound,
      rootStore: false
    });
  }

  get isLoading() {
    return this.isLoadingTemplates || this.isLoadingCategories;
  }

  *fetchTemplates() {
    this.isLoadingTemplates = true;
    try {
      const response = yield this.api.credentialTypesManager.getCredentialTypes();
      this.credentialTemplates = response.concat(this.mockedCredentialTemplates);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchTemplates',
        verb: 'getting',
        model: 'Templates'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
    this.isLoadingTemplates = false;
  }

  *getCredentialTemplateDetails(id) {
    try {
      const result = yield this.api.credentialTypesManager.getCredentialTypeDetails(id);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      return result;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'getCredentialTemplateDetails',
        verb: 'getting',
        model: 'Template Details'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
  }

  *createCredentialTemplate(newTemplate) {
    try {
      this.mockedCredentialTemplates.push(newTemplate);
      yield this.api.credentialTypesManager.createTemplate(newTemplate);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'createCredentialTemplate',
        verb: 'saving',
        model: 'Template'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
  }

  *fetchCategories() {
    this.isLoadingCategories = true;
    try {
      const response = yield this.api.credentialTypesManager.getTemplateCategories();
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.templateCategories = response.concat(this.mockedTemplateCategories);
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCategories',
        verb: 'getting',
        model: 'Template Categories'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
    this.isLoadingCategories = false;
  }

  *createTemplateCategory(newCategoryData) {
    this.isLoadingCategories = true;
    try {
      const { categoryName } = newCategoryData;
      const newCategory = { id: uuidv4(), name: categoryName, state: 1 };
      const response = yield this.api.credentialTypesManager.createCategory(newCategory);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.mockedTemplateCategories.push(newCategory);
      this.fetchCategories();
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'createTemplateCategory',
        verb: 'saving',
        model: 'Template Category'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
    this.isLoadingCategories = false;
  }
}
