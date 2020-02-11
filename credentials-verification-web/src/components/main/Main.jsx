import React from 'react';
import { message } from 'antd';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from '../Router';
import { APIContext } from '../providers/ApiContext';
import '../../App.scss';
import './_main.scss';
import { theme } from '../../helpers/themeHelper';

const MESSAGE_DURATION = 3;
const MAX_MESSAGES_TO_SHOW = 3;

message.config({
  duration: MESSAGE_DURATION,
  maxCount: MAX_MESSAGES_TO_SHOW
});

const Main = ({ apiProvider }) => (
  <main>
    <APIContext.Provider value={apiProvider}>
      <div className={`AppContainer ${theme.class()}`}>
        <Router />
      </div>
    </APIContext.Provider>
  </main>
);

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
