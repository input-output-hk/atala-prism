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

  *createCredentialTemplate(newTemplate) {
    try {
      this.mockedCredentialTemplates.push(newTemplate);
      yield this.api.credentialTypesManager.createTemplate(newTemplate);
    } catch (error) {
      const metadata = {
        customMessage: `[${
          this.storeName
        }.createCredentialTemplate] Error while creating credential template`,
        model: 'Templates'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  *fetchTemplates() {
    this.isLoadingTemplates = true;
    try {
      const response = yield this.api.credentialTypesManager.getCredentialTypes();
      this.credentialTemplates = response.concat(this.mockedCredentialTemplates);
    } catch (error) {
      const metadata = {
        customMessage: `[${this.storeName}.fetchTemplates] Error while getting templates`,
        model: 'Templates'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
    this.isLoadingTemplates = false;
  }

  *getCredentialTemplateDetails(id) {
    try {
      return yield this.api.credentialTypesManager.getCredentialTypeDetails(id);
    } catch (error) {
      const metadata = {
        customMessage: `[${
          this.storeName
        }.getCredentialTemplateDetails] Error while getting template details`,
        model: 'Template'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  *createTemplateCategory(newCategoryData) {
    try {
      const { categoryName, categoryIcon } = newCategoryData;
      const logo = categoryIcon.isCustomIcon ? categoryIcon.file.thumbUrl : categoryIcon.src;
      const newCategory = { id: uuidv4(), name: categoryName, logo, state: 1 };
      yield this.api.credentialTypesManager.createCategory(newCategory);
      this.mockedTemplateCategories.push(newCategory);
      this.fetchCategories();
    } catch (error) {
      const metadata = {
        customMessage: `[${
          this.storeName
        }.createTemplateCategory] Error while creating tempalte category`,
        model: 'Categories'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  *fetchCategories() {
    this.isLoadingCategories = true;
    try {
      const response = yield this.api.credentialTypesManager.getTemplateCategories();
      this.templateCategories = response.concat(this.mockedTemplateCategories);
    } catch (error) {
      const metadata = {
        customMessage: `[${
          this.storeName
        }.fetchCategories] Error while getting template categories`,
        model: 'Categories'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
    this.isLoadingCategories = false;
  }
}
