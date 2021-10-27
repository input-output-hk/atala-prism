import { reaction } from 'mobx';
import { useContext, useEffect, useState } from 'react';
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
  const { triggerSearch, resetState } = credentialIssuedUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.nameFilter, () => triggerSearch());
  }, [credentialIssuedUiState.nameFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.credentialTypeFilter, () => triggerSearch());
  }, [credentialIssuedUiState.credentialTypeFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.credentialStatusFilter, () => triggerSearch());
  }, [credentialIssuedUiState.credentialStatusFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.connectionStatusFilter, () => triggerSearch());
  }, [credentialIssuedUiState.connectionStatusFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.dateFilter, () => triggerSearch());
  }, [credentialIssuedUiState.dateFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.sortingKey, () => triggerSearch());
  }, [credentialIssuedUiState.sortingKey, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialIssuedUiState.sortDirection, () => triggerSearch());
  }, [credentialIssuedUiState.sortDirection, triggerSearch]);

  return credentialIssuedUiState;
};

export const useAllCredentials = () => {
  const [allCredentials, setAllCredentials] = useState([]);
  const { fetchAllCredentials, isFetching } = useCredentialIssuedStore();

  useEffect(() => {
    const triggerFetch = async () => {
      const fetchedCredentials = await fetchAllCredentials();
      setAllCredentials(fetchedCredentials);
    };
    triggerFetch();
  }, [fetchAllCredentials]);

  return {
    isLoading: isFetching,
    allCredentials
  };
};
