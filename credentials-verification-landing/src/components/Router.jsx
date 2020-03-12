import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import UserInfo from './userInfo/UserInfoContainer';
import Credentials from './credentials/CredentialsContainer';
import Contact from './contact/ContactContainer';

const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };

const landingRoute = {
  exact: true,
  path: '/',
  key: '/',
  component: Landing
};
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

const routes = [errorRoute, landingRoute, userInfoRoute, credentialsRoute, contactRoute];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
