import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

import { useUpdateEffect } from './useUpdateEffect';

export const useGroupStore = () => {
  const { rootGroupStore } = useContext(GlobalStateContext);

  return rootGroupStore.groupStore;
};

export const useGroupUiState = ({ reset } = { reset: false }) => {
  const { rootGroupStore } = useContext(GlobalStateContext);
  const {
    triggerSearch,
    resetState,
    nameFilter,
    dateFilter,
    sortingKey,
    sortDirection
  } = rootGroupStore.groupUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  const sortingAndFiltersDependencies = [nameFilter, dateFilter, sortingKey, sortDirection];

  useUpdateEffect(() => {
    triggerSearch();
  }, [...sortingAndFiltersDependencies, triggerSearch]);

  return rootGroupStore.groupUiState;
};
