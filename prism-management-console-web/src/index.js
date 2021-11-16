import React from 'react';
import ReactDOM from 'react-dom';
import './index.scss';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import * as serviceWorker from './serviceWorker';
import { config } from './APIs/config';
import { DIDBased } from './APIs/auth';
import Api, { hardcodedApi } from './APIs';
import { APIContext } from './components/providers/ApiContext';
import { GlobalStateContext, createStores } from './stores';

const supremeApi = Object.assign(new Api(config, DIDBased), hardcodedApi);
const stores = createStores(supremeApi);

ReactDOM.render(
  <APIContext.Provider value={supremeApi}>
    <GlobalStateContext.Provider value={stores}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </GlobalStateContext.Provider>
  </APIContext.Provider>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.register();
