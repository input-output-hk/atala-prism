import React, { createRef } from 'react';
import PropTypes from 'prop-types';
import { withApi } from '../providers/witApi';
import Login from './Login';

const LoginContainer = ({ api: { getDid } }) => {
  const formRef = createRef();

  const handleLogin = () => {
    formRef.current.getForm().validateFieldsAndScroll(['password'], (errors, { password }) => {
      if (errors) return;

      getDid();
    });
  };

  return <Login formRef={formRef} handleLogin={handleLogin} />;
};

LoginContainer.prototype = {
  api: PropTypes.shape().isRequired
};

export default withApi(LoginContainer);
