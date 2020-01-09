import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import './index.scss';
import Logger from './helpers/Logger';
import App from './App';
import I18nError from './components/I18nError';
import * as serviceWorker from './serviceWorker';
import i18nInitialise from './i18nInitialisator';

i18nInitialise()
  .then(() => {
    ReactDOM.render(
      <BrowserRouter>
        <App />
      </BrowserRouter>,
      document.getElementById('root')
    );
  })
  .catch(error => {
    Logger.error('[index.i18nInitialise] Error while initialising i18n', error);
    ReactDOM.render(<I18nError />, document.getElementById('root'));
  });

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
