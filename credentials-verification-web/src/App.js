import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
import { mockApi, api } from './APIs';
import { ISSUER, VERIFIER } from './helpers/constants';

/* Here should be the header and the sidebar */
const App = () => {
  localStorage.setItem('userRole', ISSUER);
  return <Main apiProvider={{ ...mockApi, ...api }} />;
};

export default withRouter(App);
