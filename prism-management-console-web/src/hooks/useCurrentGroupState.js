import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

export const useCurrentGroupState = id => {
  const { rootGroupStore } = useContext(GlobalStateContext);
  const { init } = rootGroupStore.currentGroupState;

  useEffect(() => {
    if (id) init(id);
  }, [id, init]);

  return rootGroupStore.currentGroupState;
};
