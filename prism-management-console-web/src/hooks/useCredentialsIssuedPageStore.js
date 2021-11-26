import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

export const useCredentialsIssuedPageStore = () => {
  const { credentialsIssuedPageStore } = useContext(GlobalStateContext);

  return credentialsIssuedPageStore;
};
