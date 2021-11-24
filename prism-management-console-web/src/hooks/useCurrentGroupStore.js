import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

export const useCurrentGroupStore = id => {
  const { currentGroupStore } = useContext(GlobalStateContext);
  const { init } = currentGroupStore;

  useEffect(() => {
    if (id) init(id);
  }, [id, init]);

  return currentGroupStore;
};
