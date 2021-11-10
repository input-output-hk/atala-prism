import { reaction } from 'mobx';
import { useContext, useEffect, useState } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useContactStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { contactStore } = useContext(PrismStoreContext);
  const { fetchContactsNextPage, resetContacts } = contactStore;

  useEffect(() => {
    if (reset) resetContacts();
  }, [reset, resetContacts]);

  useEffect(() => {
    if (fetch) fetchContactsNextPage();
  }, [fetch, fetchContactsNextPage]);

  return contactStore;
};

export const useContactUiState = ({ reset } = { reset: false }) => {
  const { contactUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = contactUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => contactUiState.textFilter, () => triggerSearch());
  }, [contactUiState.textFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => contactUiState.dateFilter, () => triggerSearch());
  }, [contactUiState.dateFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => contactUiState.statusFilter, () => triggerSearch());
  }, [contactUiState.statusFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => contactUiState.sortingKey, () => triggerSearch());
  }, [contactUiState.sortingKey, triggerSearch]);

  useEffect(() => {
    reaction(() => contactUiState.sortDirection, () => triggerSearch());
  }, [contactUiState.sortDirection, triggerSearch]);

  return contactUiState;
};

export const useAllContacts = () => {
  const [allContacts, setAllContacts] = useState([]);
  const { fetchAllContacts, isFetching } = useContactStore();

  useEffect(() => {
    const triggerFetch = async () => {
      const fetchedContacts = await fetchAllContacts();
      setAllContacts(fetchedContacts);
    };
    triggerFetch();
  }, [fetchAllContacts]);

  return {
    isLoading: isFetching,
    allContacts
  };
};
