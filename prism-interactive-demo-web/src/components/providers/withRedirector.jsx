import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToLanding = () => {
    redirectTo('');
  };

  const redirectToCredentials = () => {
    redirectTo('credentials');
  };

  const redirectToContact = () => {
    redirectTo('contact');
  };

  const redirector = {
    redirectToLanding,
    redirectToCredentials,
    redirectToContact
  };

  return <Component {...props} redirector={redirector} />;
};
