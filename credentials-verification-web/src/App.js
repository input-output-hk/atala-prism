import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
import { getHolders, inviteHolder } from './APIs/__mocks__/holders'; // it used just to replace App.js in src

/* Here should be the header and the sidebar */
const App = () => <Main apiProvider={{ getHolders, inviteHolder }} />;

export default withRouter(App);
