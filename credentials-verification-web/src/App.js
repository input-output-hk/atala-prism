import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
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
      getConnections
    }}
  />
);

export default withRouter(App);
