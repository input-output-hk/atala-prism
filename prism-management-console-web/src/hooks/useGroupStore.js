import { useContext, useEffect } from 'react';
import { GlobalStateContext } from '../stores';

import { useUpdateEffect } from './useUpdateEffect';

// TODO: delete when all usage is refactored
export const useGroupStore = () => {
  const { groupStore } = useContext(GlobalStateContext);

  return groupStore;
};

// TODO: delete when all usage is refactored
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

/**
 *
 * @returns {GroupsPageStore}
 */
export const useGroupsPageStore = () => {
  const { groupsPageStore } = useContext(GlobalStateContext);

  return groupsPageStore;
};

/**
 *
 * @returns {CurrentGroupStore}
 */
export const useCurrentGroupStore = () => {
  const { currentGroupStore } = useContext(GlobalStateContext);

  return currentGroupStore;
};

/**
 *
 * @returns {CreateGroupStore}
 */
export const useCreateGroupStore = () => {
  const { createGroupStore } = useContext(GlobalStateContext);

  return createGroupStore;
};
