import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToLanding = () => redirectTo('');

  const redirectToCredential = () => redirectTo('credential');

  const redirector = { redirectToLanding, redirectToCredential };

  return <Component {...props} redirector={redirector} />;
};
