import { createContext } from 'react';
import { RootGroupStore } from './RootGroupStore';
import { RootStore } from './RootStore';

export const createStores = api => {
  // TODO: spread the rest of the entity stores into separate Root<Entity>Store
  const rootStore = new RootStore(api);
  const rootGroupStore = new RootGroupStore(api, rootStore.uiState.sessionState);

  return {
    ...rootStore.uiState,
    ...rootStore.prismStore,
    rootGroupStore
  };
};

export const GlobalStateContext = createContext({});
