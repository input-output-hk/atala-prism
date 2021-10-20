import React from 'react';
import { observer } from 'mobx-react-lite';
import { message } from 'antd';
import 'antd/dist/antd.css';
import Router from '../Router';
import i18nInitialise from '../../i18nInitialise';
import I18nError from '../I18nError';
import Logger from '../../helpers/Logger';
import UnconfirmedAccountErrorModal from '../common/Organisms/Modals/UnconfirmedAccountErrorModal/UnconfirmedAccountErrorModal';
import { useSession } from '../../hooks/useSession';
import '../../App.scss';
import './_main.scss';

const MESSAGE_DURATION = 3;
const MAX_MESSAGES_TO_SHOW = 3;

message.config({
  duration: MESSAGE_DURATION,
  maxCount: MAX_MESSAGES_TO_SHOW
});

const Main = () => {
  i18nInitialise().catch(error => {
    Logger.error('[index.i18nInitialise] Error while initialising i18n', error);
    return <I18nError />;
  });

  return (
    <main>
      <MainContent />
    </main>
  );
};

const MainContent = observer(() => {
  const { modalIsVisible, hideModal } = useSession();

  return (
    <div className="AppContainer">
      <Router />
      <UnconfirmedAccountErrorModal visible={modalIsVisible} hide={hideModal} />
    </div>
  );
});

export default Main;
