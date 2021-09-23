import { reaction } from 'mobx';
import { useContext, useEffect } from 'react';
import { PrismStoreContext } from '../stores/domain/PrismStore';
import { UiStateContext } from '../stores/ui/UiState';

export const useGroupStore = ({ fetch, reset } = {}) => {
  const { groupStore } = useContext(PrismStoreContext);
  const { fetchGroupsNextPage, resetGroups } = groupStore;

  useEffect(() => {
    if (reset) resetGroups();
    if (fetch) fetchGroupsNextPage();
  }, [fetch, fetchGroupsNextPage]);

  return groupStore;
};

export const useGroupUiState = ({ reset } = {}) => {
  const { groupUiState } = useContext(UiStateContext);
  const { triggerSearch, resetState } = groupUiState;

  useEffect(() => {
    if (reset) {
      resetState();
    }
  }, [reset, resetState]);

  useEffect(() => {
    reaction(() => groupUiState.nameFilter || groupUiState.searchResults, () => triggerSearch());
  }, []);

  return groupUiState;
};
