import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/main/Main';
import { api } from './APIs';

const App = () => <Main apiProvider={{ ...api }} />;

export default withRouter(App);
