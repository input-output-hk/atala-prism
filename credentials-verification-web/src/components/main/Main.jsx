import React from 'react';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from '../Router';
import { APIContext } from '../providers/ApiContext';
import '../../App.scss';
import './_main.scss';

const Main = ({ apiProvider }) => (
  <main>
    <APIContext.Provider value={apiProvider}>
      {/* Add class IssuerUser or VerifierUser to change Theme Color */}
      <div className="AppContainer IssuerUser">
        <Router />
      </div>
    </APIContext.Provider>
  </main>
);

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
