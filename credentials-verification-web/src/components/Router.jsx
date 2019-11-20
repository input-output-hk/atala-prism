import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Dashboard from './dashboard/Dashboard';
import Landing from './landing/Landing';
import Login from './login/LoginContainer';
import I18nError from './I18nError';
import Connections from './connections/ConnectionsContainer';
import Registration from './registration/RegistrationContainer';
import NotFound from './notFound/NotFound';
import Groups from './groups/GroupsContainer';
import Credential from './credentials/CredentialContainer';
import CredentialSummaries from './credentialSummaries/CredentialSummaryController';
import NewCredential from './newCredential/NewCredentialContainer';
import Payment from './payments/PaymentContainer';
import { withSideBar } from './providers/withSideBar';

const landingRoute = { exact: true, path: '/landing', key: '/landing', component: Landing };
const loginRoute = { exact: true, path: '/login', key: '/login', component: Login };
const dashboardRoute = { exact: true, path: '/', key: '/', component: withSideBar(Dashboard) };
const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };
const connections = {
  exact: true,
  path: '/connections',
  key: '/connections',
  component: withSideBar(Connections)
};
const groups = { exact: true, path: '/groups', key: '/groups', component: withSideBar(Groups) };
const credential = {
  exact: true,
  path: '/credentials',
  key: '/credentials',
  component: withSideBar(Credential)
};
const credentialSummary = {
  exact: true,
  path: '/credentialSummary',
  key: '/credentialSummary',
  component: withSideBar(CredentialSummaries)
};
const newCredential = {
  exact: true,
  path: '/newCredential',
  key: '/newCredential',
  component: withSideBar(NewCredential)
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
  component: withSideBar(Payment)
};
const notFound = { key: 'notFound', component: NotFound };

const routes = [
  dashboardRoute,
  landingRoute,
  errorRoute,
  loginRoute,
  connections,
  groups,
  newCredential,
  credential,
  credentialSummary,
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
