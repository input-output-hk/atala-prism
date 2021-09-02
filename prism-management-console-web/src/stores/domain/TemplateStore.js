import { makeAutoObservable, observable, action, computed } from 'mobx';
import { nanoid } from 'nanoid';

export class TemplateStore {
  isLoadingTemplates = false;

  isLoadingCategories = false;

  credentialTemplates = [];

  templateCategories = [];

  mockedCredentialTemplates = [];

  mockedTemplateCategories = [];

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      isLoadingTemplates: observable,
      isLoadingCategories: observable,
      credentialTemplates: observable,
      addCredentialTemplate: action,
      fetchTemplates: action,
      getCredentialTemplateDetails: action,
      templateCategories: observable,
      addTemplateCategory: action,
      fetchCategories: action,
      isLoading: computed,
      rootStore: false
    });
  }

  get isLoading() {
    return this.isLoadingTemplates || this.isLoadingCategories;
  }

  addCredentialTemplate = async newTemplate => {
    this.mockedCredentialTemplates.push(newTemplate);
    await this.api.credentialTypesManager.createTemplate(newTemplate);
  };

  fetchTemplates = async () => {
    this.isLoadingTemplates = true;
    const response = await this.api.credentialTypesManager.getCredentialTypes();
    this.credentialTemplates = response.concat(this.mockedCredentialTemplates);
    this.isLoadingTemplates = false;
  };

  getCredentialTemplateDetails = async id =>
    this.api.credentialTypesManager.getCredentialTypeDetails(id);

  addTemplateCategory = async newCategoryData => {
    const { categoryName, categoryIcon } = newCategoryData;
    const logo = categoryIcon.isCustomIcon ? categoryIcon.file.thumbUrl : categoryIcon.src;
    const newCategory = { id: nanoid(), name: categoryName, logo, state: 1 };
    await this.api.credentialTypesManager.createCategory(newCategory);
    this.mockedTemplateCategories.push(newCategory);
    this.fetchCategories();
  };

  fetchCategories = async () => {
    this.isLoadingCategories = true;
    const response = await this.api.credentialTypesManager.getTemplateCategories();
    this.templateCategories = response.concat(this.mockedTemplateCategories);
    this.isLoadingCategories = false;
  };
}
