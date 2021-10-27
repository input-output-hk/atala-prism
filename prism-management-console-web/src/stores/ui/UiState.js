import { createContext } from 'react';
import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';
import SessionState from './SessionState';
import GroupUiState from './GroupUiState';

export class UiState {
  constructor(api, rootStore) {
    this.sessionState = new SessionState(api, rootStore);
    this.groupUiState = new GroupUiState(rootStore);
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}

export const UiStateContext = createContext({});
