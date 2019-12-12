import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/main/Main';
import { api, hardcodedApi } from './APIs';

const App = () => <Main apiProvider={{ ...api, ...hardcodedApi }} />;

export default withRouter(App);
