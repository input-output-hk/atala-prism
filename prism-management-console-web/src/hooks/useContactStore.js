import { useContext, useEffect, useState } from 'react';
import { GlobalStateContext } from '../stores/index';
import { useUpdateEffect } from './useUpdateEffect';

export const useContactStore = () => {
  const { rootContactStore } = useContext(GlobalStateContext);

  return rootContactStore.contactStore;
};

export const useContactUiState = ({ reset } = { reset: false }) => {
  const { rootContactStore } = useContext(GlobalStateContext);
  const {
    triggerSearch,
    textFilter,
    dateFilter,
    resetState,
    statusFilter,
    sortingKey,
    sortDirection
  } = rootContactStore.contactUiState;

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

  return rootContactStore.contactUiState;
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
