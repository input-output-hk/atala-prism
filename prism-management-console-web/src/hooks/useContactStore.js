import { useContext, useEffect, useState } from 'react';
import { GlobalStateContext } from '../stores/index';
import { useUpdateEffect } from './useUpdateEffect';

export const useContactStore = () => {
  const { contactStore } = useContext(GlobalStateContext);

  return contactStore;
};

export const useContactUiState = ({ instance, reset } = { reset: false }) => {
  const {
    triggerSearch,
    textFilter,
    dateFilter,
    resetState,
    statusFilter,
    sortingKey,
    sortDirection
  } = instance;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  const sortingAndFiltersDependencies = [
    textFilter,
    dateFilter,
    statusFilter,
    sortingKey,
    sortDirection,
    sortDirection
  ];

  useUpdateEffect(() => {
    triggerSearch();
  }, [...sortingAndFiltersDependencies, triggerSearch]);

  return instance;
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
