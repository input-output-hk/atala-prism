import { useEffect, useState } from 'react';
import { useApi } from './useApi';

export const useAllContacts = () => {
  const [allContacts, setAllContacts] = useState([]);
  const [isFetching, setIsFetching] = useState(false);
  const { contactsManager } = useApi();

  useEffect(() => {
    const triggerFetch = async () => {
      setIsFetching(true);

      try {
        const response = await contactsManager.getAllContacts();
        setAllContacts(response);
      } finally {
        setIsFetching(false);
      }
    };

    triggerFetch();
  }, [contactsManager]);

  return {
    isLoading: isFetching,
    allContacts
  };
};
