import { makeAutoObservable, observable, computed, action } from 'mobx';
import { v4 as uuidv4 } from 'uuid';
import { CREDENTIAL_TYPE_STATUSES } from '../../helpers/constants';
import { defaultTemplateSketch, insertFormChangeIntoArray } from '../../helpers/templateHelpers';
import {
  configureHtmlTemplate,
  getContrastColorSettings,
  updateImages
} from '../../helpers/templateLayouts/templates';

export default class TemplateUiState {
  templateSketch = defaultTemplateSketch;

  form;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      templateSketch: observable,
      form: observable,
      preview: computed,
      setSketchState: action,
      resetSketch: action,
      setForm: action,
      setTemplatePreview: action,
      createTemplateFromSketch: action,
      updateCredentialBody: false,
      rootStore: false
    });
  }

  get preview() {
    const contrastColorSettings = getContrastColorSettings(this.templateSketch);

    const currentConfig = {
      ...this.templateSketch,
      ...contrastColorSettings
    };

    const configuredHtmlTemplate = configureHtmlTemplate(currentConfig.layout, currentConfig);
    return configuredHtmlTemplate;
  }

  setSketchState = async stateChange => {
    if (stateChange.userIcon || stateChange.companyIcon) {
      this.templateSketch = {
        ...this.templateSketch,
        ...stateChange
      };
      const updatedImages = await updateImages(this.templateSketch);
      this.templateSketch = {
        ...this.templateSketch,
        ...updatedImages
      };
    } else if (stateChange.credentialBody) {
      this.updateCredentialBody(stateChange);
    } else {
      this.templateSketch = {
        ...this.templateSketch,
        ...stateChange
      };
    }
  };

  updateCredentialBody = stateChange => {
    this.templateSketch = {
      ...this.templateSketch,
      credentialBody: insertFormChangeIntoArray(
        stateChange.credentialBody,
        this.templateSketch.credentialBody
      )
    };
  };

  resetSketch = () => {
    this.templateSketch = defaultTemplateSketch;
  };

  setForm = ref => {
    this.form = ref;
  };

  createTemplateFromSketch = () => {
    const { createCredentialTemplate } = this.rootStore.prismStore.templateStore;
    const newTemplate = {
      ...this.templateSketch,
      template: this.preview,
      state: CREDENTIAL_TYPE_STATUSES.MOCKED,
      id: uuidv4()
    };
    return createCredentialTemplate(newTemplate);
  };
}
