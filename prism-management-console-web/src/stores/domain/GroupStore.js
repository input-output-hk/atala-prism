import { makeAutoObservable, flow } from 'mobx';
import { GROUP_PAGE_SIZE, MAX_GROUP_PAGE_SIZE } from '../../helpers/constants';
import GroupUiState from '../ui/GroupUiState';

const defaultValues = {
  isFetching: true,
  groups: [],
  numberOfGroups: undefined
};

const fallback = {
  groupsList: [],
  totalNumberOfGroups: undefined
};
export default class GroupStore {
  isFetching = defaultValues.isFetching;

  groups = defaultValues.groups;

  numberOfGroups = defaultValues.numberOfGroups;

  constructor(api, sessionState) {
    this.api = api;
    this.transportLayerStateHandler = sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      fetchMoreData: flow.bound,
      fetchSearchResults: flow.bound,
      getGroupsToSelect: flow.bound,
      updateGroup: flow.bound,
      fetchGroups: flow.bound,
      fetchRecursively: false,
      groupUiState: false,
      currentGroupState: false
    });
    // has to be declared after `fetchSearchResults` has been bound.
    // otherwise binding can be forced by passing this.fetchSearchResults.bind(this)
    this.groupUiState = new GroupUiState({ triggerFetchResults: this.fetchSearchResults });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.numberOfGroups === undefined;
  }

  get hasMore() {
    return this.numberOfGroups > this.groups.length;
  }

  initGroupStore = () => {
    this.resetGroups();
    this.fetchMoreData({ isInitialLoading: true });
  };

  resetGroups = () => {
    this.groups = defaultValues.groups;
    this.numberOfGroups = defaultValues.numberOfGroups;
  };

  *fetchMoreData({ isInitialLoading } = {}) {
    if (!isInitialLoading && !this.hasMore) return;
    const response = yield this.fetchGroups({ offset: isInitialLoading ? 0 : this.groups.length });
    this.groups = isInitialLoading ? response.groupsList : this.groups.concat(response.groupsList);
    this.numberOfGroups = response.numberOfGroups;
  }

  *fetchSearchResults() {
    const response = yield this.fetchGroups({ offset: 0 });
    this.resetGroups();
    this.groups = this.groups.concat(response.groupsList);
    this.numberOfGroups = response.numberOfGroups;
  }

  *getGroupsToSelect() {
    const alreadyFetched = this.groups;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively(alreadyFetched);
    this.groups = response.groupsList;

    return response.groupsList;
  }

  fetchRecursively = async (acc = []) => {
    const response = await this.fetchGroups({
      offset: acc.length,
      pageSize: MAX_GROUP_PAGE_SIZE
    });
    const updatedAcc = acc.concat(response.groupsList);
    if (updatedAcc.length >= response.totalNumberOfGroups)
      return { groupsList: updatedAcc, totalNumberOfGroups: response.totalNumberOfGroups };
    return this.fetchRecursively(updatedAcc);
  };

  *fetchGroups({ offset = 0, pageSize = GROUP_PAGE_SIZE } = {}) {
    this.isFetching = true;
    try {
      const { nameFilter, dateFilter = [], sortDirection, sortingBy } = this.groupUiState;
      const [createdAfter, createdBefore] = dateFilter;

      const response = yield this.api.groupsManager.getGroups({
        offset,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          name: nameFilter,
          createdBefore,
          createdAfter
        }
      });
      this.transportLayerStateHandler.handleTransportLayerSuccess();
      this.isFetching = false;
      return response || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchGroups',
        verb: 'getting',
        model: 'Groups'
      };
      this.transportLayerStateHandler.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }

  *updateGroup(id, change) {
    this.isSaving = true;
    try {
      const response = yield this.api.groupsManager.updateGroup(id, change);
      this.transportLayerStateHandler.handleTransportLayerSuccess();
      this.isSaving = false;
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'updateGroup',
        verb: 'saving',
        model: 'Group'
      };
      this.transportLayerStateHandler.handleTransportLayerError(error, metadata);
      this.isSaving = false;
    }
  }

  createGroup = async ({ name, members }) => {
    const newGroup = await this.api.groupsManager.createGroup(name);
    if (members) await this.updateGroup(newGroup.id, { contactIdsToAdd: members });
  };
}
