import { createContext } from 'react';
import { RootStore } from './RootStore';
import SessionState from './ui/SessionState';
import GroupStore from './domain/GroupStore';
import CurrentGroupStore from './features/CurrentGroupStore';
import ContactStore from './domain/ContactStore';
import CurrentContactState from './features/CurrentContactState';
import ContactsPageStore from './features/ContactsPageStore';
import GroupsPageStore from './features/GroupsPageStore';
import CredentialsIssuedPageStore from './features/CredentialsIssuedPageStore';

export const createStores = api => {
  const sessionState = new SessionState(api);
  // TODO: spread the rest of the entity stores into separate Root<Entity>Store
  const rootStore = new RootStore(api, sessionState);
  const groupStore = new GroupStore(api, sessionState);
  const groupsPageStore = new GroupsPageStore(api, sessionState);
  const currentGroupStore = new CurrentGroupStore(api, sessionState);
  const contactStore = new ContactStore(api, sessionState);
  const contactsPageStore = new ContactsPageStore(api, sessionState);
  const currentContactState = new CurrentContactState(api, sessionState);
  const credentialsIssuedPageStore = new CredentialsIssuedPageStore(api, sessionState);

  return {
    ...rootStore.uiState,
    ...rootStore.prismStore,
    sessionState,
    groupStore,
    groupsPageStore,
    currentGroupStore,
    contactStore,
    contactsPageStore,
    currentContactState,
    credentialsIssuedPageStore
  };
};

export const GlobalStateContext = createContext({});
