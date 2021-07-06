import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from './withApi';
import {
  SESSION,
  LOADING,
  LOCKED,
  UNLOCKED,
  CONFIRMED,
  UNCONFIRMED
} from '../../helpers/constants';
import UnconfirmedAccountErrorModal from '../common/Organisms/Modals/UnconfirmedAccountErrorModal/UnconfirmedAccountErrorModal';

const SessionContext = React.createContext();

const SessionProviderComponent = props => {
  const {
    api: { wallet }
  } = props;

  const { t } = useTranslation();

  const [session, setSession] = useState({ sessionState: LOADING });
  const [accountStatus, setAccountStatus] = useState(LOADING);
  const [acceptedModal, setAcceptedModal] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);

  const login = useCallback(
    () =>
      wallet.getSessionFromExtension({ timeout: 5000 }).then(({ sessionData, error }) => {
        if (error) {
          setSession({ sessionState: LOCKED });
          message.error(t(error.message));
        } else setSession(sessionData);
      }),
    [t, wallet]
  );

  const logout = useCallback(() => {
    setSession({ sessionState: LOCKED });
    setAcceptedModal(false);
  }, []);

  useEffect(() => {
    const handleSessionError = () => {
      message.error(t('errors.walletLockedOrRemoved'));
      logout();
    };

    wallet.setSessionErrorHandler(handleSessionError);
    const storedSession = JSON.parse(localStorage.getItem(SESSION));
    if (storedSession?.sessionState === UNLOCKED) login();
    else setSession({ sessionState: LOCKED });
  }, [login, t, wallet, logout]);

  useEffect(() => {
    localStorage.setItem(SESSION, JSON.stringify(session));
    wallet.setSessionState(session);
  }, [session, wallet]);

  const verifyRegistration = () => wallet.verifyRegistration({ timeout: 5000 });

  const removeUnconfirmedAccountError = useCallback(() => setAccountStatus(CONFIRMED), []);

  const showUnconfirmedAccountError = useCallback(() => {
    setAccountStatus(UNCONFIRMED);
    if (!acceptedModal) {
      setModalVisible(true);
    }
  }, [acceptedModal]);

  const hideModal = () => {
    setAcceptedModal(true);
    setModalVisible(false);
  };

  return (
    <>
      <SessionContext.Provider
        value={{
          session,
          login,
          logout,
          verifyRegistration,
          showUnconfirmedAccountError,
          removeUnconfirmedAccountError,
          accountStatus
        }}
        {...props}
      />
      <UnconfirmedAccountErrorModal visible={modalVisible} hide={hideModal} />
    </>
  );
};

SessionProviderComponent.propTypes = {
  api: PropTypes.shape({
    wallet: PropTypes.shape({
      getSessionFromExtension: PropTypes.func.isRequired,
      verifyRegistration: PropTypes.func.isRequired,
      setSessionState: PropTypes.func.isRequired,
      setSessionErrorHandler: PropTypes.func.isRequired
    }).isRequired
  }).isRequired
};

const SessionProvider = withApi(SessionProviderComponent);

const useSession = () => React.useContext(SessionContext);

export { SessionProvider, useSession };
