import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';
import { useUpdateEffect } from './useUpdateEffect';

export const useGroupStore = () => {
  const { groupStore } = useContext(PrismStoreContext);

  return groupStore;
};

export const useGroupUiState = ({ reset } = { reset: false }) => {
  const { groupUiState } = useContext(UiStateContext);
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
