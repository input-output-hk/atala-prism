import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

export const useCurrentContactState = contactId => {
  const { currentContactState } = useContext(GlobalStateContext);
  const { init } = currentContactState;

  useEffect(() => {
    if (contactId) init(contactId);
  }, [contactId, init]);

  return currentContactState;
};
