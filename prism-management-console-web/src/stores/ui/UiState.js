import { createContext } from 'react';
import SessionState from './SessionState';
import ContactUiState from './ContactUiState';
import GroupUiState from './GroupUiState';
import CurrentGroupState from './CurrentGroupState';
import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';
import CredentialIssuedUiState from './CredentialIssuedUiState';
import CredentialReceivedUiState from './CredentialReceivedUiState';

export class UiState {
  constructor(api, rootStore) {
    this.sessionState = new SessionState(api, rootStore);
    this.contactUiState = new ContactUiState(rootStore);
    this.groupUiState = new GroupUiState(rootStore);
    this.currentGroupState = new CurrentGroupState(rootStore);
    this.credentialIssuedUiState = new CredentialIssuedUiState(rootStore);
    this.credentialReceivedUiState = new CredentialReceivedUiState(rootStore);
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}

export const UiStateContext = createContext({});
