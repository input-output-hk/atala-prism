import { makeAutoObservable } from 'mobx';
import TemplateBaseStore from '../domain/TemplateBaseStore';

export default class CredentialsIssuedPageStore {
  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.templatesBaseStore = new TemplateBaseStore(api, sessionState);

    makeAutoObservable(
      this,
      {
        api: false,
        sessionState: false,
        templatesBaseStore: false
      },
      { autoBind: true }
    );
  }

  get credentialTemplates() {
    return this.templatesBaseStore.credentials;
  }

  get templateCategories() {
    return this.templatesBaseStore.templateCategories;
  }

  get filteredTemplates() {
    return this.templatesBaseStore.filteredTemplates;
  }

  get filterSortingProps() {
    return this.templatesBaseStore.filterSortingProps;
  }

  get isLoading() {
    return this.templatesBaseStore.isLoading;
  }

  initTemplateStore() {
    return this.templatesBaseStore.initTemplateStore();
  }
}
