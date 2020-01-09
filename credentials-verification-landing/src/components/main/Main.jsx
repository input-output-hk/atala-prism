import React from 'react';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from '../Router';
import { APIContext } from '../providers/ApiContext';
import '../../css/app.scss';
import './_main.scss';

const Main = ({ apiProvider }) => (
  <main>
    <APIContext.Provider value={apiProvider}>
      <div className="AppContainer">
        <Router />
      </div>
    </APIContext.Provider>
  </main>
);

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
