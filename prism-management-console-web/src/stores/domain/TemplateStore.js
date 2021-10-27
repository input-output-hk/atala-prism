import { makeAutoObservable, observable, computed, flow } from 'mobx';
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
    this.storeName = this.constructor.name;
    makeAutoObservable(this, {
      isLoadingTemplates: observable,
      isLoadingCategories: observable,
      credentialTemplates: observable,
      templateCategories: observable,
      fetchTemplates: flow.bound,
      getCredentialTemplateDetails: flow.bound,
      createCredentialTemplate: flow.bound,
      fetchCategories: flow.bound,
      createTemplateCategory: flow.bound,
      isLoading: computed,
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
      this.rootStore.handleTransportLayerSuccess();
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchTemplates',
        verb: 'getting',
        model: 'Templates'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
    this.isLoadingTemplates = false;
  }

  *getCredentialTemplateDetails(id) {
    try {
      const result = yield this.api.credentialTypesManager.getCredentialTypeDetails(id);
      this.rootStore.handleTransportLayerSuccess();
      return result;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'getCredentialTemplateDetails',
        verb: 'getting',
        model: 'Template Details'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  *createCredentialTemplate(newTemplate) {
    try {
      this.mockedCredentialTemplates.push(newTemplate);
      yield this.api.credentialTypesManager.createTemplate(newTemplate);
      this.rootStore.handleTransportLayerSuccess();
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'createCredentialTemplate',
        verb: 'saving',
        model: 'Template'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  *fetchCategories() {
    this.isLoadingCategories = true;
    try {
      const response = yield this.api.credentialTypesManager.getTemplateCategories();
      this.rootStore.handleTransportLayerSuccess();
      this.templateCategories = response.concat(this.mockedTemplateCategories);
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCategories',
        verb: 'getting',
        model: 'Template Categories'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
    this.isLoadingCategories = false;
  }

  *createTemplateCategory(newCategoryData) {
    this.isLoadingCategories = true;
    try {
      const { categoryName } = newCategoryData;
      const newCategory = { id: uuidv4(), name: categoryName, state: 1 };
      const response = yield this.api.credentialTypesManager.createCategory(newCategory);
      this.rootStore.handleTransportLayerSuccess();
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
      this.rootStore.handleTransportLayerError(error, metadata);
    }
    this.isLoadingCategories = false;
  }
}
