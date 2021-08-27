import { createContext } from 'react';

export class UiState {
  credentialTemplate = {
    filters: {
      name: 'go'
    }
  };

  constructor(rootStore) {
    this.rootStore = rootStore;
  }
}

export const UiStateContext = createContext({});
