import React from 'react';
import { navigate } from 'gatsby';

const redirectTo = route => navigate(`/app/${route}`);

const redirectToLanding = () => navigate('/');

const redirectToCredentials = () => redirectTo('credentials');

const redirectToContact = () => redirectTo('contact');

export const redirector = {
  redirectToLanding,
  redirectToCredentials,
  redirectToContact
};

export const withRedirector = Component => props => (
  <Component {...props} redirector={redirector} />
);
