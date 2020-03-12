import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/main/Main';
import Api, { hardcodedApi } from './APIs';
import { config } from './APIs/config';
import { Legacy } from './APIs/auth';

const supremeApi = Object.assign(new Api(config, Legacy), hardcodedApi);
const App = () => <Main apiProvider={supremeApi} />;

export default withRouter(App);
