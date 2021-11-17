import { useContext, useEffect } from 'react';
import { useUpdateEffect } from './useUpdateEffect';
import { GlobalStateContext } from '../stores';

export const useCredentialIssuedStore = ({ reset } = { reset: false }) => {
  const { credentialIssuedStore } = useContext(GlobalStateContext);
  const { resetCredentials } = credentialIssuedStore;

  useEffect(() => {
    if (reset) resetCredentials();
  }, [reset, resetCredentials]);

  return credentialIssuedStore;
};

export const useCredentialIssuedUiState = ({ reset } = { reset: false }) => {
  const { credentialIssuedUiState } = useContext(GlobalStateContext);
  const {
    triggerSearch,
    resetState,
    searchTextFilter,
    credentialTypeFilter,
    credentialStatusFilter,
    connectionStatusFilter,
    dateFilter,
    sortingKey,
    sortDirection
  } = credentialIssuedUiState;

  const sortingAndFiltersDependencies = [
    searchTextFilter,
    credentialTypeFilter,
    credentialStatusFilter,
    connectionStatusFilter,
    dateFilter,
    sortingKey,
    sortDirection
  ];

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useUpdateEffect(() => {
    triggerSearch();
  }, [triggerSearch, ...sortingAndFiltersDependencies]);

  return credentialIssuedUiState;
};
