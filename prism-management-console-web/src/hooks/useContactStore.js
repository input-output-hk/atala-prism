import { reaction } from 'mobx';
import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useContactStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { contactStore } = useContext(PrismStoreContext);
  const { fetchContactsNextPage, resetContacts } = contactStore;

  useEffect(() => {
    if (reset) resetContacts();
    if (fetch) fetchContactsNextPage();
  }, [reset, fetch, resetContacts, fetchContactsNextPage]);

  return contactStore;
};

export const useContactUiState = ({ reset } = { reset: false }) => {
  const { contactUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = contactUiState;

  useEffect(() => {
    if (reset) {
      resetState();
    }
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => contactUiState.textFilter, () => triggerSearch());
    reaction(() => contactUiState.dateFilter, () => triggerSearch());
    reaction(() => contactUiState.statusFilter, () => triggerSearch());
    reaction(() => contactUiState.sortingKey, () => triggerSearch());
    reaction(() => contactUiState.sortDirection, () => triggerSearch());
  }, [
    contactUiState.textFilter,
    contactUiState.dateFilter,
    contactUiState.statusFilter,
    contactUiState.sortingKey,
    contactUiState.sortDirection,
    triggerSearch
  ]);

  return contactUiState;
};
