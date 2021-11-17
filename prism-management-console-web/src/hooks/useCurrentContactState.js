import { useContext, useEffect } from 'react';
import { UiStateContext } from '../stores/ui/UiState';

export const useCurrentContactState = contactId => {
  const { currentContactState } = useContext(UiStateContext);
  const { init } = currentContactState;

  useEffect(() => {
    if (contactId) init(contactId);
  }, [contactId, init]);

  return currentContactState;
};
