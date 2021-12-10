import { createContext } from 'react';
import SessionState from './ui/SessionState';
import CurrentGroupStore from './features/CurrentGroupStore';
import CurrentContactState from './features/CurrentContactState';
import ContactsPageStore from './features/ContactsPageStore';
import GroupsPageStore from './features/GroupsPageStore';
import CreateGroupStore from './features/CreateGroupStore';
import CredentialsIssuedPageStore from './features/CredentialsIssuedPageStore';
import CredentialsReceivedStore from './domain/CredentialsReceivedStore';
import CreateCredentialPageStore from './features/CeateCredentialPageStore';
import TemplatesPageStore from './features/TemplatesPageStore';
import TemplatesByCategoryStore from './features/TemplatesByCategoryStore';
import TemplateCreationStore from './features/TemplateCreationStore';

export const createStores = api => {
  const sessionState = new SessionState(api);
  const groupsPageStore = new GroupsPageStore(api, sessionState);
  const currentGroupStore = new CurrentGroupStore(api, sessionState);
  const createGroupStore = new CreateGroupStore(api, sessionState);
  const contactsPageStore = new ContactsPageStore(api, sessionState);
  const currentContactState = new CurrentContactState(api, sessionState);
  const credentialsIssuedPageStore = new CredentialsIssuedPageStore(api, sessionState);
  const credentialsReceivedStore = new CredentialsReceivedStore(api, sessionState);
  const createCredentialPageStore = new CreateCredentialPageStore(api, sessionState);
  const templatesPageStore = new TemplatesPageStore(api, sessionState);
  const templatesByCategoryStore = new TemplatesByCategoryStore(api, sessionState);
  const templateCreationStore = new TemplateCreationStore(api, sessionState);

  return {
    sessionState,
    groupsPageStore,
    currentGroupStore,
    createGroupStore,
    contactsPageStore,
    currentContactState,
    credentialsIssuedPageStore,
    credentialsReceivedStore,
    createCredentialPageStore,
    templatesPageStore,
    templatesByCategoryStore,
    templateCreationStore
  };
};

export const GlobalStateContext = createContext({});
