import { reaction } from 'mobx';
import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useCredentialReceivedStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { credentialReceivedStore } = useContext(PrismStoreContext);
  const { fetchCredentialsNextPage, resetCredentials } = credentialReceivedStore;

  useEffect(() => {
    if (reset) resetCredentials();
  }, [reset, resetCredentials]);

  useEffect(() => {
    if (fetch) fetchCredentialsNextPage();
  }, [fetch, fetchCredentialsNextPage]);

  return credentialReceivedStore;
};

export const useCredentialReceivedUiState = ({ reset } = { reset: false }) => {
  const { credentialReceivedUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = credentialReceivedUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.nameFilter, () => triggerSearch());
  }, [credentialReceivedUiState.nameFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.credentialTypeFilter, () => triggerSearch());
  }, [credentialReceivedUiState.credentialTypeFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.credentialStatusFilter, () => triggerSearch());
  }, [credentialReceivedUiState.credentialStatusFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.connectionStatusFilter, () => triggerSearch());
  }, [credentialReceivedUiState.connectionStatusFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.dateFilter, () => triggerSearch());
  }, [credentialReceivedUiState.dateFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.sortingKey, () => triggerSearch());
  }, [credentialReceivedUiState.sortingKey, triggerSearch]);

  useEffect(() => {
    reaction(() => credentialReceivedUiState.sortDirection, () => triggerSearch());
  }, [credentialReceivedUiState.sortDirection, triggerSearch]);

  return credentialReceivedUiState;
};
