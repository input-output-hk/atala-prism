import React, { createContext, useReducer, useEffect } from 'react';
import { setStoredItem, getStoredItem, removeStoredItem } from './Storage';
import { USER } from '../../helpers/constants';

const initialState = {
  admissionDate: '2019-02-03T17:23:21.909Z',
  startDate: '2019-02-03T17:23:21.909Z',
  graduationDate: '2020-02-03T17:23:21.909Z',
  issuerId: '091d41cc-e8fc-4c44-9bd3-c938dcf76dff'
};

const reducer = (user, newUser) => {
  if (!newUser) {
    removeStoredItem(USER);
    return initialState;
  }
  return { ...user, ...newUser };
};

const localState = JSON.parse(getStoredItem(USER));

const UserContext = createContext({});

const UserProvider = ({ children }) => {
  const [user, setUser] = useReducer(reducer, localState || initialState);

  useEffect(() => {
    setStoredItem(USER, JSON.stringify(user));
  }, [user]);

  return <UserContext.Provider value={{ user, setUser }}>{children}</UserContext.Provider>;
};

export { UserContext, UserProvider };
