import React from 'react';
import Main from './components/main/Main';
import { api } from './APIs';
import i18nInitialise from './i18nInitialisator';

i18nInitialise();

const App = () => <Main apiProvider={{ ...api }} />;

export default App;
