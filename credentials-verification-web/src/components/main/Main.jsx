import React from 'react';
import { message } from 'antd';
import 'antd/dist/antd.css';
import PropTypes from 'prop-types';
import Router from '../Router';
import { APIContext } from '../providers/ApiContext';
import { SessionProvider, useSession } from '../providers/SessionContext';
import { getThemeByRole } from '../../helpers/themeHelper';
import i18nInitialise from '../../i18nInitialisator';
import I18nError from '../I18nError';
import Logger from '../../helpers/Logger';
import '../../App.scss';
import './_main.scss';

const MESSAGE_DURATION = 3;
const MAX_MESSAGES_TO_SHOW = 3;

message.config({
  duration: MESSAGE_DURATION,
  maxCount: MAX_MESSAGES_TO_SHOW
});

const Main = ({ apiProvider }) => {
  i18nInitialise().catch(error => {
    Logger.error('[index.i18nInitialise] Error while initialising i18n', error);
    return <I18nError />;
  });

  return (
    <main>
      <APIContext.Provider value={apiProvider}>
        <SessionProvider>
          <MainContent />
        </SessionProvider>
      </APIContext.Provider>
    </main>
  );
};

const MainContent = () => {
  const { session } = useSession();
  const theme = getThemeByRole(session?.userRole);
  return (
    <div className={`AppContainer ${theme.class()}`}>
      <Router />
    </div>
  );
};

Main.propTypes = {
  apiProvider: PropTypes.shape().isRequired
};

export default Main;
