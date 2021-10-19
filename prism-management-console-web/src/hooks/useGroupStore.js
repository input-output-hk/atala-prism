import { reaction } from 'mobx';
import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useGroupStore = ({ fetch, reset } = { fetch: false, reset: false }) => {
  const { groupStore } = useContext(PrismStoreContext);
  const { fetchGroupsNextPage, resetGroups } = groupStore;

  useEffect(() => {
    if (reset) resetGroups();
  }, [reset, resetGroups]);

  useEffect(() => {
    if (fetch) fetchGroupsNextPage();
  }, [fetch, fetchGroupsNextPage]);

  return groupStore;
};

export const useGroupUiState = ({ reset } = { reset: false }) => {
  const { groupUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = groupUiState;

  useEffect(() => {
    if (reset) resetState();
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => groupUiState.nameFilter, () => triggerSearch());
  }, [groupUiState.nameFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => groupUiState.dateFilter, () => triggerSearch());
  }, [groupUiState.dateFilter, triggerSearch]);

  useEffect(() => {
    reaction(() => groupUiState.sortingKey, () => triggerSearch());
  }, [groupUiState.sortingKey, triggerSearch]);

  useEffect(() => {
    reaction(() => groupUiState.sortDirection, () => triggerSearch());
  }, [groupUiState.sortDirection, triggerSearch]);

  return groupUiState;
};
