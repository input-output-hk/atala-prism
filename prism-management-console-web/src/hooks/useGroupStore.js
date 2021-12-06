import { useContext } from 'react';
import { GlobalStateContext } from '../stores';

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
