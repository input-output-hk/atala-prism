import React from 'react';
import { navigate } from 'gatsby';

export const withRedirector = Component => props => {
  const redirectTo = route => navigate(`/app/${route}`);

  const redirectToLanding = () => redirectTo('');

  const redirectToCredentials = () => redirectTo('credentials');

  const redirectToContact = () => redirectTo('contact');

  const redirector = {
    redirectToLanding,
    redirectToCredentials,
    redirectToContact
  };

  return <Component {...props} redirector={redirector} />;
};
