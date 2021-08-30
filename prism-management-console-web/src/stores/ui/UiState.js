import { createContext } from 'react';
import { TemplateUiState } from './TemplateUiState';

export class UiState {
  constructor(rootStore) {
    this.templateUiState = new TemplateUiState(rootStore);
  }
}

export const UiStateContext = createContext({});
