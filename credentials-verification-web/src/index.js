import React from 'react';
import ReactDOM from 'react-dom';
import './index.scss';
import App from './App';
import I18nError from './I18nError';
import * as serviceWorker from './serviceWorker';
import i18nInitialise from './i18nInitialisator';

i18nInitialise().then(() => {
  ReactDOM.render(<App />, document.getElementById('root'));
}).catch(() => {
  ReactDOM.render(<I18nError />, document.getElementById('root'));
});

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
