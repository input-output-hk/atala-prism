import React, { createContext, useReducer, useEffect } from 'react';
import { setStoredItem, getStoredItem, removeStoredItem } from './Storage';
import { USER } from '../../helpers/constants';

const initialState = {};

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
