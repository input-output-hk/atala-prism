import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
import { geConnectionToken } from './APIs/connector/connector';
import { getHolders, inviteHolder } from './APIs/__mocks__/holders';
import { getGroups, deleteGroup } from './APIs/__mocks__/groups';
import {
  getCredentials,
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getTotalCredentials
} from './APIs/__mocks__/credentials';
import { getConnections } from './APIs/__mocks__/connections';
import {
  savePictureInS3,
  saveCredential,
  saveDraft
} from './APIs/__mocks__/credentialInteractions';

/* Here should be the header and the sidebar */
const App = () => (
  <Main
    apiProvider={{
      getHolders,
      inviteHolder,
      getCredentials,
      getCredentialTypes,
      getCategoryTypes,
      getCredentialsGroups,
      getGroups,
      deleteGroup,
      getTotalCredentials,
      getConnections,
      savePictureInS3,
      saveCredential,
      saveDraft,
      geConnectionToken
    }}
  />
);

export default withRouter(App);
