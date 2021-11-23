import { useContext, useEffect, useState } from 'react';
import { GlobalStateContext } from '../stores/index';

export const useContactStore = () => {
  const { contactStore } = useContext(GlobalStateContext);

  return contactStore;
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
