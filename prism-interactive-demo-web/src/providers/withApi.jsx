import React, { useContext } from 'react';
import { APIContext } from './ApiContext';

export const withApi = Component => props => {
  const api = useContext(APIContext);
  return <Component {...props} api={api} />;
};
