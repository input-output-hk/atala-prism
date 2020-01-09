import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import Credential from './credential/CredentialContainer';

const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };

const landingRoute = {
  exact: true,
  path: '/',
  key: '/',
  component: Landing
};
const credentialRoute = {
  path: '/credential',
  key: 'credential',
  component: Credential
};

const routes = [errorRoute, landingRoute, credentialRoute];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
