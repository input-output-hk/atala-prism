import { makeAutoObservable } from 'mobx';
import { defaultTemplateSketch, insertFormChangeIntoArray } from '../../helpers/templateHelpers';
import {
  configureHtmlTemplate,
  getContrastColorSettings
} from '../../helpers/templateLayouts/templates';

export default class TemplateSketchStore {
  templateSketch = defaultTemplateSketch;

  form;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
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

    return configureHtmlTemplate(currentConfig.layout, currentConfig);
  }

  resetSketch() {
    this.templateSketch = defaultTemplateSketch;
  }

  setSketchState(stateChange) {
    if (stateChange.credentialBody) {
      this.updateCredentialBody(stateChange);
    } else {
      this.templateSketch = {
        ...this.templateSketch,
        ...stateChange
      };
    }
  }

  updateCredentialBody(stateChange) {
    this.templateSketch = {
      ...this.templateSketch,
      credentialBody: insertFormChangeIntoArray(
        stateChange.credentialBody,
        this.templateSketch.credentialBody
      )
    };
  }

  setForm(ref) {
    this.form = ref;
  }
}
