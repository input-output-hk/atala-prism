import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

import { useUpdateEffect } from './useUpdateEffect';

export const useGroupStore = () => {
  const { groupStore } = useContext(GlobalStateContext);

  return groupStore;
};

export const useGroupUiState = ({ reset } = { reset: false }) => {
  const { groupUiState } = useGroupStore();
  const {
    triggerSearch,
    resetState,
    nameFilter,
    dateFilter,
    sortingKey,
    sortDirection
  } = groupUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  const sortingAndFiltersDependencies = [nameFilter, dateFilter, sortingKey, sortDirection];

  useUpdateEffect(() => {
    triggerSearch();
  }, [...sortingAndFiltersDependencies, triggerSearch]);

  return groupUiState;
};
