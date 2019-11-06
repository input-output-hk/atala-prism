import React, { createRef } from 'react';
import { withApi } from '../providers/witApi';
import Login from './Login';

const LoginContainer = ({ api: { getDid } }) => {
  const formRef = createRef();

  const handleLogin = () => {
    console.log('the waletterino', getDid);
    formRef.current.getForm().validateFieldsAndScroll(['password'], (errors, { password }) => {
      if (errors) return;

      getDid();
    });
  };

  return <Login formRef={formRef} handleLogin={handleLogin} />;
};

export default withApi(LoginContainer);
