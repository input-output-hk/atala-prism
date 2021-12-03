import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

/**
 *
 * @returns {CreateCredentialPageStore}
 */
export const useCreateCredentialPageStore = () => {
  const { createCredentialPageStore } = useContext(GlobalStateContext);

  return createCredentialPageStore;
};
