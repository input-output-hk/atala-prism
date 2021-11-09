import { useContext, useEffect, useRef } from 'react';
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

  const isInitialMount = useRef(true);

  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
    } else {
      triggerSearch();
    }
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
