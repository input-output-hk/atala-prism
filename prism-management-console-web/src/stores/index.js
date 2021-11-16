import { createContext } from 'react';
import { RootGroupStore } from './RootGroupStore';
import { RootStore } from './RootStore';
import SessionState from './ui/SessionState';

export const createStores = api => {
  const sessionState = new SessionState(api);
  // TODO: spread the rest of the entity stores into separate Root<Entity>Store
  const rootStore = new RootStore(api, sessionState);
  const rootGroupStore = new RootGroupStore(api, sessionState);

  return {
    ...rootStore.uiState,
    ...rootStore.prismStore,
    sessionState,
    rootGroupStore
  };
};

export const GlobalStateContext = createContext({});
