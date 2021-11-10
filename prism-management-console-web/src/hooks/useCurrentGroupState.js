import { useContext, useEffect } from 'react';
import { UiStateContext } from '../stores/ui/UiState';

export const useCurrentGroupState = id => {
  const { currentGroupState } = useContext(UiStateContext);
  const { init } = currentGroupState;

  useEffect(() => {
    if (id) init(id);
  }, [id, init]);

  return currentGroupState;
};
