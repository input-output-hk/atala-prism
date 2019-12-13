import React, { createRef } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';
import { withApi } from '../providers/witApi';
import Login from './Login';
import Logger from '../../helpers/Logger';
import { UNLOCKED, translateStatus, MISSING_WALLET_ERROR } from '../../helpers/constants';

const LoginContainer = ({ api: { getDid, unlockWallet } }) => {
  const formRef = createRef();
  const { t } = useTranslation();
  const history = useHistory();

  const handleLogin = () => {
    formRef.current.getForm().validateFieldsAndScroll(['password'], (errors, { password }) => {
      if (errors) return;
      unlockWallet(password)
        .then(status => {
          if (translateStatus(status) === UNLOCKED) {
            history.push('/');
            return;
          }
          message.error(t('errors.invalidPassword'));
        })
        .catch(error => {
          Logger.error(error);
          if (error.message === MISSING_WALLET_ERROR) {
            message.error(t('errors.noWallet'));
          } else {
            message.error(t('errors.invalidPassword'));
          }
        });
    });
  };

  return <Login formRef={formRef} handleLogin={handleLogin} />;
};

LoginContainer.propTypes = {
  api: PropTypes.shape({
    getDid: PropTypes.func,
    unlockWallet: PropTypes.func
  }).isRequired
};

export default withApi(LoginContainer);
