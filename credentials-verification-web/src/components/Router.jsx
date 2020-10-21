import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Dashboard from './dashboard/Dashboard';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import Contacts from './connections/ConnectionsContainer';
import Registration from './registration/RegistrationContainer';
import Groups from './groups/GroupsContainer';
import Credential from './credentials/CredentialContainer';
import NewCredential from './newCredential/NewCredentialContainer';
import Payment from './payments/PaymentContainer';
import Settings from './settings/SettingsContainer';
import IndividualCreation from './individualCreation/IndividualCreationContainer';
import Admin from './admin/AdminContainer';
import { withSideBar } from './providers/withSideBar';
import { ISSUER, VERIFIER } from '../helpers/constants';
import { withLoggedValidation } from './providers/withLoggedValidation';
import GroupCreationContainer from './groupCreation/GroupCreationContainer';
import Instructions from './instructions/instructions';
import ImportContactsContainer from './importContacts/ImportContactsContainer';

const issuer = [ISSUER];
const verifier = [VERIFIER];
const allRoles = [ISSUER, VERIFIER];
const noRole = [];

const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };

const contacts = {
  exact: true,
  path: '/contacts',
  key: '/contacts',
  component: withLoggedValidation(withSideBar(Contacts), allRoles)
};
const groups = {
  exact: true,
  path: '/groups',
  key: '/groups',
  component: withLoggedValidation(withSideBar(Groups), issuer)
};
const credential = {
  exact: true,
  path: '/credentials',
  key: '/credentials',
  component: withLoggedValidation(withSideBar(Credential), issuer)
};
const newCredential = {
  exact: true,
  path: '/credentials/create',
  key: '/credentials/create',
  component: withLoggedValidation(withSideBar(NewCredential), issuer)
};
const registration = {
  exact: true,
  path: '/registration',
  key: '/registration',
  component: withLoggedValidation(Registration, noRole)
};
const payment = {
  exact: true,
  path: '/payment',
  key: 'payment',
  component: withLoggedValidation(withSideBar(Payment), allRoles)
};
const landingRoute = {
  path: '/landing',
  key: '/landing',
  component: withLoggedValidation(Landing, noRole)
};
const dashboardRoute = {
  exact: true,
  path: '/',
  key: 'dashboard',
  component: withLoggedValidation(withSideBar(Dashboard), allRoles)
};
const settings = {
  exact: true,
  path: '/settings',
  key: 'settings',
  component: withLoggedValidation(withSideBar(Settings), allRoles)
};
const individualCreation = {
  exact: true,
  path: '/individualCreation',
  key: 'individualCreation',
  component: withLoggedValidation(withSideBar(IndividualCreation), verifier)
};
const adminRoute = {
  exact: true,
  path: '/admin',
  key: 'admin',
  component: withLoggedValidation(withSideBar(Admin), allRoles)
};
const groupCreationRoute = {
  exact: true,
  path: '/groups/creation',
  key: 'groupsCreation',
  component: withLoggedValidation(withSideBar(GroupCreationContainer), allRoles)
};
const instructions = {
  exact: true,
  path: '/instructions',
  key: 'instructions',
  component: withLoggedValidation(Instructions, noRole)
};
const importContacts = {
  exact: true,
  path: '/contacts/import',
  key: 'contacts/import',
  component: withLoggedValidation(withSideBar(ImportContactsContainer), issuer)
};

const routes = [
  adminRoute,
  errorRoute,
  contacts,
  groupCreationRoute,
  groups,
  credential,
  settings,
  newCredential,
  registration,
  payment,
  individualCreation,
  landingRoute,
  instructions,
  importContacts,
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
