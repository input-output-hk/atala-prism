import React from 'react';
import Main from './components/main/Main';
import { api } from './APIs';

const App = () => <Main apiProvider={{ ...api }} />;

export default App;
