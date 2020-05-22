import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Landing from './landing/Landing';
import I18nError from './I18nError';
import UserInfo from './userInfo/UserInfoContainer';
import Credentials from './credentials/CredentialsContainer';
import Contact from './contact/ContactContainer';
import TermsAndConditions from './legal/TermsAndConditions';
import PrivacyPolicy from './legal/PrivacyPolicy';

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
const termsRoute = {
  path: '/terms-and-conditions',
  key: 'terms-and-conditions',
  component: TermsAndConditions
};
const privacyRoute = {
  path: '/privacy-policy',
  key: 'privacy-policy',
  component: PrivacyPolicy
};

const routes = [
  errorRoute,
  landingRoute,
  userInfoRoute,
  credentialsRoute,
  contactRoute,
  termsRoute,
  privacyRoute
];

const Router = () => (
  <Switch>
    {routes.map(route => (
      <Route {...route} />
    ))}
  </Switch>
);

export default Router;
