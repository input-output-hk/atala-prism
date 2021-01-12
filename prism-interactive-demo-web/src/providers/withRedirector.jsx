import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToAskCredential = () => redirectTo('askCredential');

  const redirectToHome = () => redirectTo('');

  const redirector = { redirectToAskCredential, redirectToHome };

  return <Component {...props} redirector={redirector} />;
};
