import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import Recipients from './recipients/RecipientsContainer';
import NotFound from './notFound/NotFound';
import Groups from './groups/GroupsContainer';
import Credential from './credentials/CredentialContainer';
import Connections from './connections/ConnectionsController';
import NewCredential from './newCredential/NewCredentialContainer';

const landingRoute = { exact: true, path: '/', key: '/', component: Landing };
const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };
const recipients = { exact: true, path: '/recipients', key: '/recipients', component: Recipients };
const groups = { exact: true, path: '/groups', key: '/groups', component: Groups };
const credential = {
  exact: true,
  path: '/credentials',
  key: '/credentials',
  component: Credential
};
const connections = {
  exact: true,
  path: '/connections',
  key: '/connections',
  component: Connections
};
const newCredential = {
  exact: true,
  path: '/newCredential',
  key: '/newCredential',
  component: NewCredential
};
const notFound = { key: 'notFound', component: NotFound };

const routes = [
  landingRoute,
  errorRoute,
  recipients,
  groups,
  newCredential,
  credential,
  connections,
  notFound
];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
