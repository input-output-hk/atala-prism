import React from 'react';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from './Router';
import { APIContext } from './providers/ApiContext';
import Header from './common/Molecules/Header/Header';
import SideMenu from './common/Molecules/SideBar/SideBar';

import './_main.scss';

const Main = ({ apiProvider }) => (
  <main>
    <APIContext.Provider value={apiProvider}>
      <div className="AppContainer">
        <Header />
        <div className="MainContent">
          <SideMenu />
          <div className="MainContainer">
            <Router />
          </div>
        </div>
      </div>
    </APIContext.Provider>
  </main>
);

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
