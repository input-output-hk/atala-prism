import { useContext, useEffect, useState } from 'react';
import { GlobalStateContext } from '../stores/index';

export const useContactStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { contactStore } = useContext(GlobalStateContext);
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
  const { contactUiState } = useContext(GlobalStateContext);
  const { triggerSearch, resetState, statusFilter, sortingKey, sortDirection } = contactUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    triggerSearch();
  }, [statusFilter, sortingKey, sortDirection, sortDirection, triggerSearch]);

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
