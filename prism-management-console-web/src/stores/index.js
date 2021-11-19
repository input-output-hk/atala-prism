import { createContext } from 'react';
import { RootStore } from './RootStore';
import SessionState from './ui/SessionState';
import GroupStore from './domain/GroupStore';
import CurrentGroupStore from './ui/CurrentGroupStore';
import ContactStore from './domain/ContactStore';
import CurrentContactState from './ui/CurrentContactState';
import ContactsBaseStore from './ContactsBaseStore';

export const createStores = api => {
  const sessionState = new SessionState(api);
  // TODO: spread the rest of the entity stores into separate Root<Entity>Store
  const rootStore = new RootStore(api, sessionState);
  const groupStore = new GroupStore(api, sessionState);
  const currentGroupStore = new CurrentGroupStore(api, sessionState);
  const contactStore = new ContactStore(api, sessionState);
  const contactsPageStore = new ContactsBaseStore(api, sessionState);
  const currentContactState = new CurrentContactState(api, sessionState);

  return {
    ...rootStore.uiState,
    ...rootStore.prismStore,
    sessionState,
    groupStore,
    currentGroupStore,
    contactStore,
    contactsPageStore,
    currentContactState
  };
};

export const GlobalStateContext = createContext({});
