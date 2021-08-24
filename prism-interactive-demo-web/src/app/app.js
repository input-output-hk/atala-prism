import React, { Suspense, useEffect, useState } from 'react';
import Main from './components/main/Main';
import { api } from './APIs';
import { FirebaseAppProvider } from 'reactfire';
import i18nInitialise from './i18nInitialisator';
import { config } from './APIs/configs';
import SEO from '../components/seo/seo';

i18nInitialise();

const App = () => {
  // const [firebaseConfig, setFirebaseConfig] = useState({});

  // useEffect(() => {
  // const { REACT_APP_FIREBASE_CONFIG } = window._env_;
  // const newFirebaseConfig = JSON.parse(REACT_APP_FIREBASE_CONFIG);
  // setFirebaseConfig(newFirebaseConfig);
  // }, []);
  console.log(config.firebaseConfig)

  return (
    <FirebaseAppProvider firebaseConfig={JSON.parse(config.firebaseConfig)}>
      <SEO />
      <Suspense fallback="">
        <Main apiProvider={{ ...api }} />
      </Suspense>
    </FirebaseAppProvider>
  );
};

export default App;
