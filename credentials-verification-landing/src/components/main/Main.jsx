import React from 'react';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from '../Router';
import { APIContext } from '../providers/ApiContext';
import { UserProvider } from '../providers/userContext';
import '../../css/app.scss';
import './_main.scss';

const Main = ({ apiProvider }) => (
  <main>
    <UserProvider>
      <APIContext.Provider value={apiProvider}>
        <div className="AppContainer">
          <Router />
        </div>
      </APIContext.Provider>
    </UserProvider>
  </main>
);

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
