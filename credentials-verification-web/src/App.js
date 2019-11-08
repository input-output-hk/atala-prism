import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
import { mockApi, api } from './APIs';

/* Here should be the header and the sidebar */
const App = () => <Main apiProvider={{ ...mockApi, ...api }} />;

export default withRouter(App);
