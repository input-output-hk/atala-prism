import { useContext } from 'react';
import { UiStateContext } from '../stores/ui/UiState';

export const useSession = () => {
  const { sessionState } = useContext(UiStateContext);

  return sessionState;
};
