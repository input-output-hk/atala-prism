import React from 'react';
import ReactDOM from 'react-dom';
import './index.scss';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import * as serviceWorker from './serviceWorker';
import { config } from './APIs/config';
import { DIDBased } from './APIs/auth';
import Api, { hardcodedApi } from './APIs';
import { PrismStoreContext } from './stores/domain/PrismStore';
import { APIContext } from './components/providers/ApiContext';
import { RootStore } from './stores/RootStore';
import { UiStateContext } from './stores/ui/UiState';

const supremeApi = Object.assign(new Api(config, DIDBased), hardcodedApi);
const rootStore = new RootStore(supremeApi);

ReactDOM.render(
  <APIContext.Provider value={supremeApi}>
    <PrismStoreContext.Provider value={rootStore.prismStore}>
      <UiStateContext.Provider value={rootStore.uiState}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </UiStateContext.Provider>
    </PrismStoreContext.Provider>
  </APIContext.Provider>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.register();
