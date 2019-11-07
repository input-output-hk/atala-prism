import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Dashboard from './dashboard/Dashboard';
import Landing from './landing/Landing';
import Login from './login/LoginContainer';
import I18nError from './I18nError';
import Recipients from './recipients/RecipientsContainer';
import Registration from './registration/RegistrationContainer';
import NotFound from './notFound/NotFound';
import Groups from './groups/GroupsContainer';
import Credential from './credentials/CredentialContainer';
import Connections from './connections/ConnectionsController';
import NewCredential from './newCredential/NewCredentialContainer';
import Payment from './payments/PaymentContainer';

const landingRoute = { exact: true, path: '/', key: '/', component: Landing };
const loginRoute = { exact: true, path: '/login', key: '/login', component: Login };
const dashboardRoute = { exact: true, path: '/', key: '/', component: Dashboard };
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
const registration = {
  exact: true,
  path: '/registration',
  key: '/registration',
  component: Registration
};
const payment = {
  exact: true,
  path: '/payment',
  key: 'paymnt',
  component: Payment
};
const notFound = { key: 'notFound', component: NotFound };

const routes = [
  dashboardRoute,
  landingRoute,
  errorRoute,
  loginRoute,
  recipients,
  groups,
  newCredential,
  credential,
  connections,
  registration,
  payment,
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
