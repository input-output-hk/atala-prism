import React, { Suspense } from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/main/Main';
import { api } from './APIs';
import { FirebaseAppProvider } from 'reactfire';
import firebaseConfig from './firebase-config';

const App = () => (
  <FirebaseAppProvider firebaseConfig={firebaseConfig}>
    <Suspense fallback="" >
      <Main apiProvider={{ ...api }} />
    </Suspense>
  </FirebaseAppProvider>
);

export default withRouter(App);
