import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

/**
 *
 * @returns {CredentialsReceivedStore}
 */
export const useCredentialsReceivedStore = () => {
  const { credentialsReceivedStore } = useContext(GlobalStateContext);

  return credentialsReceivedStore;
};
