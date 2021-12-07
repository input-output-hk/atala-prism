import { createContext } from 'react';
import { RootStore } from './RootStore';
import SessionState from './ui/SessionState';
import CurrentGroupStore from './features/CurrentGroupStore';
import CurrentContactState from './features/CurrentContactState';
import ContactsPageStore from './features/ContactsPageStore';
import GroupsPageStore from './features/GroupsPageStore';
import CreateGroupStore from './features/CreateGroupStore';
import CredentialsIssuedPageStore from './features/CredentialsIssuedPageStore';
import CredentialsReceivedStore from './domain/CredentialsReceivedStore';
import CreateCredentialPageStore from './features/CeateCredentialPageStore';

export const createStores = api => {
  const sessionState = new SessionState(api);
  // TODO: spread the rest of the entity stores into separate Root<Entity>Store
  const rootStore = new RootStore(api, sessionState);
  const groupsPageStore = new GroupsPageStore(api, sessionState);
  const currentGroupStore = new CurrentGroupStore(api, sessionState);
  const createGroupStore = new CreateGroupStore(api, sessionState);
  const contactsPageStore = new ContactsPageStore(api, sessionState);
  const currentContactState = new CurrentContactState(api, sessionState);
  const credentialsIssuedPageStore = new CredentialsIssuedPageStore(api, sessionState);
  const credentialsReceivedStore = new CredentialsReceivedStore(api, sessionState);
  const createCredentialPageStore = new CreateCredentialPageStore(api, sessionState);

  return {
    ...rootStore.uiState,
    ...rootStore.prismStore,
    sessionState,
    groupsPageStore,
    currentGroupStore,
    createGroupStore,
    contactsPageStore,
    currentContactState,
    credentialsIssuedPageStore,
    credentialsReceivedStore,
    createCredentialPageStore
  };
};

export const GlobalStateContext = createContext({});
