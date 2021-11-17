import { useContext, useEffect } from 'react';
import { useUpdateEffect } from './useUpdateEffect';
import { GlobalStateContext } from '../stores';

export const useCredentialReceivedStore = ({ reset } = { reset: false }) => {
  const { credentialReceivedStore } = useContext(GlobalStateContext);
  const { resetCredentials } = credentialReceivedStore;

  useEffect(() => {
    if (reset) resetCredentials();
  }, [reset, resetCredentials]);

  return credentialReceivedStore;
};

export const useCredentialReceivedUiState = ({ reset } = { reset: false }) => {
  const { credentialReceivedUiState } = useContext(GlobalStateContext);
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
  } = credentialReceivedUiState;

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
  }, [...sortingAndFiltersDependencies, triggerSearch]);

  return credentialReceivedUiState;
};
