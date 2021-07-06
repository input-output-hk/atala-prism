import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Dashboard from './dashboard/Dashboard';
import I18nError from './I18nError';
import Contacts from './connections/ConnectionsContainer';
import Groups from './groups/GroupsContainer';
import Credential from './credentials/CredentialContainer';
import NewCredential from './newCredential/NewCredentialContainer';
import Admin from './admin/AdminContainer';
import { withSideBar } from './providers/withSideBar';
import { withLoggedValidation } from './providers/withLoggedValidation';
import Contact from './contactDetail/ContactContainer';
import GroupCreationContainer from './groupCreation/GroupCreationContainer';
import Instructions from './instructions/instructions';
import ImportContactsContainer from './importContacts/ImportContactsContainer';
import GroupEditingContainer from './groupEditing/GroupEditingContainer';

const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };

const contact = {
  exact: true,
  path: '/contacts/:id',
  key: '/contacts/:id',
  component: withLoggedValidation(withSideBar(Contact))
};
const contacts = {
  exact: true,
  path: '/contacts',
  key: '/contacts',
  component: withLoggedValidation(withSideBar(Contacts))
};
const groups = {
  exact: true,
  path: '/groups',
  key: '/groups',
  component: withLoggedValidation(withSideBar(Groups))
};
const credential = {
  exact: true,
  path: '/credentials',
  key: '/credentials',
  component: withLoggedValidation(withSideBar(Credential))
};
const newCredential = {
  exact: true,
  path: '/credentials/create',
  key: '/credentials/create',
  component: withLoggedValidation(withSideBar(NewCredential))
};
const dashboardRoute = {
  path: '/',
  key: 'dashboard',
  component: withLoggedValidation(withSideBar(Dashboard))
};
const adminRoute = {
  exact: true,
  path: '/admin',
  key: 'admin',
  component: withLoggedValidation(withSideBar(Admin))
};
const groupCreationRoute = {
  exact: true,
  path: '/groups/creation',
  key: 'groupsCreation',
  component: withLoggedValidation(withSideBar(GroupCreationContainer))
};
const groupEditingRoute = {
  exact: true,
  path: '/groups/:id',
  key: 'groupsEditing',
  component: withLoggedValidation(withSideBar(GroupEditingContainer))
};
const instructions = {
  exact: true,
  path: '/instructions',
  key: 'instructions',
  component: Instructions
};
const importContacts = {
  exact: true,
  path: '/contacts/import',
  key: 'contacts/import',
  component: withLoggedValidation(withSideBar(ImportContactsContainer))
};

const routes = [
  adminRoute,
  errorRoute,
  contacts,
  importContacts,
  contact,
  groupCreationRoute,
  groupEditingRoute,
  groups,
  credential,
  newCredential,
  instructions,
  dashboardRoute
];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
