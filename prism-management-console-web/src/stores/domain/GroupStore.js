import { makeAutoObservable, observable, flow, computed, action } from 'mobx';
import { GROUP_PAGE_SIZE } from '../../helpers/constants';

export default class GroupStore {
  isLoading = false;

  groups = [];

  numberOfGroups = 0;

  searchResults = [];

  numberOfResults = 0;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      isLoading: observable,
      groups: observable,
      searchResults: observable,
      isLoadingFirstPage: computed,
      hasMore: computed,
      resetGroups: action,
      fetchGroupsNextPage: action,
      fetchSearchResultsNextPage: action,
      fetchGroups: flow.bound,
      fetchSearchResults: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isLoading && this.numberOfGroups === 0;
  }

  get hasMore() {
    return this.numberOfGroups > this.groups.length;
  }

  get hasMoreResults() {
    return this.numberOfResults > this.searchResults.length;
  }

  resetGroups = () => {
    this.groups = [];
    this.numberOfGroups = 0;
  };

  fetchGroupsNextPage = async () => {
    this.isLoading = true;
    const { groupsList, totalNumberOfGroups } = await this.fetchGroups({
      offset: this.groups.length
    });

    this.groups = this.groups.concat(groupsList);
    this.numberOfGroups = totalNumberOfGroups;
    this.isLoading = false;
  };

  *fetchGroups({ offset = 0 }) {
    try {
      const {
        nameFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.groupUiState;
      const [createdAfter, createdBefore] = dateFilter;

      const response = yield this.api.groupsManager.getGroups({
        offset,
        pageSize: GROUP_PAGE_SIZE,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          name: nameFilter,
          createdBefore,
          createdAfter
        }
      });
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchGroups',
        verb: 'getting',
        model: 'Groups'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
    }
  }

  fetchSearchResults = async () => {
    this.searchResults = [];
    this.numberOfResults = 0;
    const { groupsList, totalNumberOfGroups } = await this.fetchGroups({ offset: 0 });
    this.searchResults = groupsList;
    this.numberOfResults = totalNumberOfGroups;
    return this.searchResults;
  };

  fetchSearchResultsNextPage = async () => {
    if (!this.hasMoreResults) return;
    const { groupsList = [] } = await this.fetchGroups({
      offset: this.searchResults.length
    });
    this.searchResults = this.searchResults.concat(groupsList);
  };
}
