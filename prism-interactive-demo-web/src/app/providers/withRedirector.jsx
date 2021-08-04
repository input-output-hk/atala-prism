import React from 'react';
import { navigate } from 'gatsby';

export const withRedirector = Component => props => {
  const redirectTo = route => navigate(`/app/${route}`);

  const redirectToAskCredential = () => redirectTo('askCredential');

  const redirectToHome = () => redirectTo('');

  const redirector = { redirectToAskCredential, redirectToHome };

  return <Component {...props} redirector={redirector} />;
};
