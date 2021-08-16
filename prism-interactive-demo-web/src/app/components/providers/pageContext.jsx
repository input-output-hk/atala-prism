import React, { createContext, useReducer, useEffect } from 'react';
import { setStoredItem, getStoredItem, removeStoredItem } from './Storage';
import { PAGE } from '../../helpers/constants';

const initialState = {};

const reducer = (page, newPage) => {
  if (!newPage) {
    removeStoredItem(PAGE);
    return initialState;
  }
  return { ...page, ...newPage };
};

const PageContext = createContext({});

const PageProvider = ({ children }) => {
  const [page, setPage] = useReducer(reducer, initialState);

  useEffect(() => {
    const localState = JSON.parse(getStoredItem(PAGE));
    setPage(localState);
  }, []);

  useEffect(() => {
    setStoredItem(PAGE, JSON.stringify(page));
  }, [page]);

  return <PageContext.Provider value={{ page, setPage }}>{children}</PageContext.Provider>;
};

export { PageContext, PageProvider };
