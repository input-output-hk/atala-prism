import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

export const useSession = () => {
  const { sessionState } = useContext(GlobalStateContext);

  return sessionState;
};
