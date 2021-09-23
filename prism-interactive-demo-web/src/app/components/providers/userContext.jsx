import React, { createContext, useReducer, useEffect } from 'react';
import { setStoredItem, getStoredItem, removeStoredItem } from './Storage';
import { USER } from '../../../helpers/constants';

const initialState = {};

const reducer = (user, newUser) => {
  if (!newUser) {
    removeStoredItem(USER);
    return initialState;
  }
  return { ...user, ...newUser };
};

const UserContext = createContext({});

const UserProvider = ({ children }) => {
  const [user, setUser] = useReducer(reducer, initialState);

  useEffect(() => {
    const localState = JSON.parse(getStoredItem(USER));
    setUser(localState);
  }, []);

  useEffect(() => {
    setStoredItem(USER, JSON.stringify(user));
  }, [user]);

  return <UserContext.Provider value={{ user, setUser }}>{children}</UserContext.Provider>;
};

export { UserContext, UserProvider };
