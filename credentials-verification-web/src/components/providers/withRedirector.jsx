import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToNewCredential = () => redirectTo('newCredential');

  const redirectToRegistration = () => redirectTo('registration');

  const redirectToLogin = () => redirectTo('login');

  const redirectToCredentials = () => redirectTo('credentials');

  const redirectToConnections = () => redirectTo('connections');

  const redirector = {
    redirectToNewCredential,
    redirectToRegistration,
    redirectToLogin,
    redirectToCredentials,
    redirectToConnections
  };

  return <Component {...props} redirector={redirector} />;
};
