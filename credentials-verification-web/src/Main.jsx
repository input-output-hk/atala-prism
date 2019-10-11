import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './pages/Landing/Landing';
import I18nError from './I18nError';
import NotFound from './pages/NotFound/NotFound';

const landingRoute = { exact: true, path: '/', key: '/', component: Landing };
const errorRoute = { exact: true, path: '/error', key: '/error', component: I18nError };
const notFound = { key: 'notFound', component: NotFound };

const routes = [landingRoute, errorRoute, notFound];

const Main = () => (
  <main>
    <Switch>
      {routes.map(route => (
        <Route {...route} />
      ))}
    </Switch>
  </main>
);

export default Main;
