import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useCredentialIssuedStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { credentialIssuedStore } = useContext(PrismStoreContext);
  const { fetchCredentialsNextPage, resetCredentials } = credentialIssuedStore;

  useEffect(() => {
    if (reset) resetCredentials();
  }, [reset, resetCredentials]);

  useEffect(() => {
    if (fetch) fetchCredentialsNextPage();
  }, [fetch, fetchCredentialsNextPage]);

  return credentialIssuedStore;
};

export const useCredentialIssuedUiState = ({ reset } = { reset: false }) => {
  const { credentialIssuedUiState } = useContext(UiStateContext);
  const {
    triggerSearch,
    resetState,
    nameFilter,
    credentialTypeFilter,
    credentialStatusFilter,
    connectionStatusFilter,
    dateFilter,
    sortingKey,
    sortDirection
  } = credentialIssuedUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    triggerSearch();
  }, [
    nameFilter,
    credentialTypeFilter,
    credentialStatusFilter,
    connectionStatusFilter,
    dateFilter,
    sortingKey,
    sortDirection,
    triggerSearch
  ]);

  return credentialIssuedUiState;
};
