import React from 'react';
import logo from './logo.svg';
import './App.scss';
import { useTranslation } from 'react-i18next';

const App = () => {
  const { t } = useTranslation();
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          {t('home.centralText.edit')}<code>{t('home.centralText.filePath')}</code>{t('home.centralText.toReload')}
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          {t('home.learnReact')}
        </a>
      </header>
    </div>
  );
}

export default App;
