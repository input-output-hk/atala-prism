import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToLanding = () => redirectTo('');

  const redirectToUserInfo = () => redirectTo('userInfo');

  const redirectToCredentials = () => {
    redirectTo('credentials');
    window.location.reload(true);
  };

  const redirectToContact = () => redirectTo('contact');

  const redirector = {
    redirectToLanding,
    redirectToUserInfo,
    redirectToCredentials,
    redirectToContact
  };

  return <Component {...props} redirector={redirector} />;
};
