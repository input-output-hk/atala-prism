import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from './withApi';
import { LOADING, SESSION } from '../../helpers/constants';

const SessionContext = React.createContext();

const SessionProviderComponent = props => {
  const {
    api: { wallet }
  } = props;

  const { t } = useTranslation();

  const [session, setSession] = useState({ sessionState: LOADING });

  useEffect(() => {
    const storedSession = localStorage.getItem(SESSION);
    if (storedSession) {
      login();
    } else {
      setSession(null);
    }
  }, []);

  useEffect(() => {
    if (session) {
      localStorage.setItem(SESSION, JSON.stringify(session));
    } else {
      localStorage.removeItem(SESSION);
    }
  });

  const login = async () => {
    const { sessionData, error } = await wallet.getSessionFromExtension({ timeout: 5000 });
    if (error) {
      setSession(null);
      message.error(t(error.message));
      return;
    }
    setSession(sessionData);
    return sessionData;
  };

  const logout = () => {
    wallet.clearSession();
    setSession(null);
  };

  const verifyRegistration = () => wallet.verifyRegistration({ timeout: 5000 });

  return (
    <SessionContext.Provider value={{ session, login, logout, verifyRegistration }} {...props} />
  );
};

SessionProviderComponent.propTypes = {
  api: PropTypes.shape({
    wallet: PropTypes.shape({
      getSessionFromExtension: PropTypes.func.isRequired,
      clearSession: PropTypes.func.isRequired,
      verifyRegistration: PropTypes.func.isRequired
    }).isRequired
  }).isRequired
};

const SessionProvider = withApi(SessionProviderComponent);

const useSession = () => React.useContext(SessionContext);

export { SessionProvider, useSession };
