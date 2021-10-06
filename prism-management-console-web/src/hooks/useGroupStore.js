import { reaction } from 'mobx';
import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useGroupStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { groupStore } = useContext(PrismStoreContext);
  const { fetchGroupsNextPage, resetGroups } = groupStore;

  useEffect(() => {
    if (reset) resetGroups();
    if (fetch) fetchGroupsNextPage();
  }, [reset, fetch, resetGroups, fetchGroupsNextPage]);

  return groupStore;
};

export const useGroupUiState = ({ reset } = { reset: false }) => {
  const { groupUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = groupUiState;

  useEffect(() => {
    if (reset) {
      resetState();
    }
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => groupUiState.nameFilter, () => triggerSearch());
    reaction(() => groupUiState.dateFilter, () => triggerSearch());
    reaction(() => groupUiState.sortingKey, () => triggerSearch());
    reaction(() => groupUiState.sortDirection, () => triggerSearch());
  }, [
    groupUiState.nameFilter,
    groupUiState.dateFilter,
    groupUiState.sortingKey,
    groupUiState.sortDirection,
    triggerSearch
  ]);

  return groupUiState;
};
