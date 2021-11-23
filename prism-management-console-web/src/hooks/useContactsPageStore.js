import { useContext } from 'react';
import { GlobalStateContext } from '../stores/index';

export const useContactsPageStore = () => {
  const { contactsPageStore } = useContext(GlobalStateContext);

  return contactsPageStore;
};
