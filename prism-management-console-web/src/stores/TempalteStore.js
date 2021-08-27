import { makeAutoObservable, observable, computed, action } from 'mobx';

export class TemplateStore {
  isLoading = false;

  credentialTemplates = [];

  mockedCredentialTemplates = [];

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      isLoading: observable,
      credentialTemplates: observable,
      filteredCredentialTemplates: computed,
      addCredentialTemplate: action,
      fetchTemplates: action,
      rootStore: false
    });
  }

  addCredentialTemplate = newTemplate => {
    this.mockedCredentialTemplates.push(newTemplate);
  };

  fetchTemplates = async () => {
    this.isLoading = true;
    const response = await this.api.credentialTypesManager.getCredentialTypes();
    this.credentialTemplates = response.concat(this.mockedCredentialTemplates);
    this.isLoading = false;
  };
}
