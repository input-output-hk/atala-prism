import { useContext, useEffect } from 'react';
import { useUpdateEffect } from './useUpdateEffect';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useCredentialIssuedStore = ({ reset } = { reset: false }) => {
  const { credentialIssuedStore } = useContext(PrismStoreContext);
  const { resetCredentials } = credentialIssuedStore;

  useEffect(() => {
    if (reset) resetCredentials();
  }, [reset, resetCredentials]);

  return credentialIssuedStore;
};

export const useCredentialIssuedUiState = ({ reset } = { reset: false }) => {
  const { credentialIssuedUiState } = useContext(UiStateContext);
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
  }, [...sortingAndFiltersDependencies, triggerSearch]);

  return credentialIssuedUiState;
};
