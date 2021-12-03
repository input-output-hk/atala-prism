import { v4 as uuidv4 } from 'uuid';
import { makeAutoObservable } from 'mobx';
import TemplatesBaseStore from '../domain/TemplatesBaseStore';
import TemplateSketchStore from './TemplateSketchStore';
import { CREDENTIAL_TYPE_STATUSES } from '../../helpers/constants';

export default class TemplateCreationStore {
  constructor(api, sessionState) {
    this.api = api;
    this.storeName = this.constructor.name;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.templatesBaseStore = new TemplatesBaseStore(api, sessionState);
    this.templateSketchStore = new TemplateSketchStore();

    makeAutoObservable(
      this,
      {
        api: false,
        transportLayerErrorHandler: false,
        templatesBaseStore: false
      },
      { autoBind: true }
    );
  }

  get form() {
    return this.templateSketchStore.form;
  }

  get templateSketch() {
    return this.templateSketchStore.templateSketch;
  }

  get preview() {
    return this.templateSketchStore.preview;
  }

  get credentialTemplates() {
    return this.templatesBaseStore.templateCategories;
  }

  get templateCategories() {
    return this.templatesBaseStore.templateCategories;
  }

  get isLoadingCategories() {
    return this.templatesBaseStore.isLoadingCategories;
  }

  *init() {
    yield this.templateSketchStore.initSketch();
    yield this.templatesBaseStore.initTemplateStore();
  }

  setForm(ref) {
    return this.templateSketchStore.setForm(ref);
  }

  setSketchState(args) {
    return this.templateSketchStore.setSketchState(args);
  }

  *createCredentialTemplate() {
    try {
      const newTemplate = {
        ...this.templateSketchStore.templateSketch,
        template: this.preview,
        state: CREDENTIAL_TYPE_STATUSES.MOCKED,
        id: uuidv4()
      };
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

  *createTemplateCategory(newCategoryData) {
    this.isFetchingCategories = true;
    try {
      const { categoryName } = newCategoryData;
      const newCategory = { id: uuidv4(), name: categoryName, state: 1 };
      const response = yield this.api.credentialTypesManager.createCategory(newCategory);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.templatesBaseStore.fetchCategories();
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
    this.isFetchingCategories = false;
  }
}
