import ContactUiState from './ContactUiState';
import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';
import CredentialIssuedUiState from './CredentialIssuedUiState';
import CredentialReceivedUiState from './CredentialReceivedUiState';
import CurrentContactState from './CurrentContactState';

export class UiState {
  constructor(api, rootStore) {
    this.contactUiState = new ContactUiState(rootStore);
    this.currentContactState = new CurrentContactState(api, rootStore);
    this.credentialIssuedUiState = new CredentialIssuedUiState(rootStore);
    this.credentialReceivedUiState = new CredentialReceivedUiState(rootStore);
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}
