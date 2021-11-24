import { useContext } from 'react';
import { GlobalStateContext } from '../stores/index';

/**
 *
 * @returns {GroupsPageStore}
 */
export const useGroupsPageStore = () => {
  const { groupsPageStore } = useContext(GlobalStateContext);

  return groupsPageStore;
};
