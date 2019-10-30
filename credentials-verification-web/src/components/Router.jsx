import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import Recipients from './recipients/RecipientsContainer';
import NotFound from './notFound/NotFound';
import Groups from './groups/GroupsContainer';

const landingRoute = { exact: true, path: '/', key: '/', component: Landing };
const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };
const recipients = { exact: true, path: '/recipients', key: '/recipients', component: Recipients };
const groups = { exact: true, path: '/groups', key: '/groups', component: Groups };
const notFound = { key: 'notFound', component: NotFound };

const routes = [landingRoute, errorRoute, recipients, groups, notFound];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
