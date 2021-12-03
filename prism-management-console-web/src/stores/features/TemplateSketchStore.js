import { makeAutoObservable } from 'mobx';
import { svgPathToEncodedBase64 } from '../../helpers/genericHelpers';
import { defaultTemplateSketch, insertFormChangeIntoArray } from '../../helpers/templateHelpers';
import {
  configureHtmlTemplate,
  getContrastColorSettings
} from '../../helpers/templateLayouts/templates';

export default class TemplateSketchStore {
  templateSketch = defaultTemplateSketch;

  form;

  constructor() {
    makeAutoObservable(
      this,
      {
        api: false,
        updateCredentialBody: false,
        credentialsIssuedBaseStore: false
      },
      { autoBind: true }
    );
  }

  get preview() {
    const contrastColorSettings = getContrastColorSettings(this.templateSketch);

    const currentConfig = {
      ...this.templateSketch,
      ...contrastColorSettings
    };

    return configureHtmlTemplate(currentConfig.layout, currentConfig);
  }

  *initSketch() {
    this.templateSketch = defaultTemplateSketch;
    this.templateSketch.icon = yield svgPathToEncodedBase64(this.templateSketch.icon);
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
