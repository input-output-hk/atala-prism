import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';
import CredentialReceivedUiState from './CredentialReceivedUiState';

export class UiState {
  constructor(api, rootStore) {
    this.credentialReceivedUiState = new CredentialReceivedUiState(rootStore);
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}
