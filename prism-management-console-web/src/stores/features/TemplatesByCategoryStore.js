import { makeAutoObservable } from 'mobx';
import { computedFn } from 'mobx-utils';
import TemplatesBaseStore from '../domain/TemplatesBaseStore';

export default class TemplatesByCategoryStore {
  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.templatesBaseStore = new TemplatesBaseStore(api, sessionState);

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

  filteredTemplatesByCategory = computedFn(category =>
    this.filteredTemplates.filter(ct => category.id === ct.category)
  );

  getCredentialTemplateDetails(id) {
    return this.templatesBaseStore.getCredentialTemplateDetails(id);
  }

  init() {
    return this.templatesBaseStore.initTemplateStore();
  }
}
