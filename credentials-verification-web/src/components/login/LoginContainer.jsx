import React, { createRef } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';
import { withApi } from '../providers/witApi';
import Login from './Login';
import Logger from '../../helpers/Logger';

const LoginContainer = ({ api: { getDid, unlockWallet } }) => {
  const formRef = createRef();
  const { t } = useTranslation();
  const history = useHistory();

  const handleLogin = () => {
    formRef.current.getForm().validateFieldsAndScroll(['password'], (errors, { password }) => {
      if (errors) return;
      unlockWallet(password)
        .then(() => {
          history.push('/');
        })
        .catch(error => {
          Logger.error(error);
          message.error(t('errors.invalidPassword'));
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
