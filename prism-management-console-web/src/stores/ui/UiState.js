import { createContext } from 'react';
import SessionState from './SessionState';
import ContactUiState from './ContactUiState';
import GroupUiState from './GroupUiState';
import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';

export class UiState {
  constructor(api, rootStore) {
    this.sessionState = new SessionState(api, rootStore);
    this.contactUiState = new ContactUiState(rootStore);
    this.groupUiState = new GroupUiState(rootStore);
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}

export const UiStateContext = createContext({});
