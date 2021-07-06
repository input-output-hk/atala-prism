// This file is used to replace App.js in src with this one which use a test api
import React from 'react';
import { withRouter } from 'react-router-dom';
// eslint-disable-next-line import/no-unresolved
import Main from './Main';
// eslint-disable-next-line import/no-unresolved
import { getHolders, inviteHolder } from './__tests__/APIs/holdersTest';

/* Here should be the header and the sidebar */
const App = () => <Main apiProvider={{ getHolders, inviteHolder }} />;

export default withRouter(App);
