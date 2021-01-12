import React, { Suspense } from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/main/Main';
import { api } from './APIs';
import { FirebaseAppProvider } from 'reactfire';

const { REACT_APP_FIREBASE_CONFIG } = window._env_;
const firebaseConfig = JSON.parse(REACT_APP_FIREBASE_CONFIG);

const App = () => (
  <FirebaseAppProvider firebaseConfig={firebaseConfig}>
    <Suspense fallback="">
      <Main apiProvider={{ ...api }} />
    </Suspense>
  </FirebaseAppProvider>
);

export default withRouter(App);
