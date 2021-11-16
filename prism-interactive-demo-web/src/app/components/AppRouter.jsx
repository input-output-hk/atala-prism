import React from 'react';
import { Router } from '@reach/router';
import I18nError from './I18nError';
import UserInfo from './userInfo/UserInfoContainer';
import Credentials from './credentials/CredentialsContainer';
import Contact from './contact/ContactContainer';

const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };

const userInfoRoute = {
  path: '/userInfo',
  key: 'userInfo',
  component: UserInfo
};
const credentialsRoute = {
  path: '/credentials',
  key: 'credentials',
  component: Credentials
};
const contactRoute = {
  path: '/contact',
  key: 'contact',
  component: Contact
};

const routes = [errorRoute, userInfoRoute, credentialsRoute, contactRoute];

const AppRouter = () => (
  <Router basepath="/app">
    {routes.map(({ component: Component, ...props }) => (
      <Component {...props} />
    ))}
  </Router>
);

export default AppRouter;
