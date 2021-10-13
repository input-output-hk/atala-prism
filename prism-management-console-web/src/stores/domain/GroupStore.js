import { makeAutoObservable, observable, flow, computed, action, runInAction } from 'mobx';
import { GROUP_PAGE_SIZE, MAX_GROUP_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isFetching: false,
  groups: [],
  numberOfGroups: undefined,
  searchResults: [],
  numberOfResults: undefined
};

const fallback = {
  groupsList: [],
  totalNumberOfGroups: undefined
};
export default class GroupStore {
  isFetching = defaultValues.isFetching;

  groups = defaultValues.groups;

  numberOfGroups = defaultValues.numberOfGroups;

  searchResults = defaultValues.searchResults;

  numberOfResults = defaultValues.numberOfResults;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      isFetching: observable,
      groups: observable,
      searchResults: observable,
      isLoadingFirstPage: computed,
      hasMore: computed,
      hasMoreGroups: computed,
      hasMoreResults: computed,
      fetchMoreData: computed,
      resetGroups: action,
      fetchGroupsNextPage: flow.bound,
      fetchSearchResults: flow.bound,
      fetchSearchResultsNextPage: flow.bound,
      updateFetchedResults: action,
      fetchAllGroups: flow.bound,
      fetchAllFilteredGroups: flow.bound,
      fetchGroups: action,
      fetchRecursively: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.numberOfGroups === undefined;
  }

  get hasMore() {
    const { hasFiltersApplied } = this.rootStore.uiState.groupUiState;
    return hasFiltersApplied ? this.hasMoreResults : this.hasMoreGroups;
  }

  get hasMoreGroups() {
    return this.numberOfGroups > this.groups.length;
  }

  get hasMoreResults() {
    return this.numberOfResults > this.searchResults.length;
  }

  get fetchMoreData() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.groupUiState;
    return hasFiltersApplied || hasCustomSorting
      ? this.fetchSearchResultsNextPage
      : this.fetchGroupsNextPage;
  }

  resetGroups = () => {
    this.isFetching = defaultValues.isFetching;
    this.groups = defaultValues.groups;
    this.numberOfGroups = defaultValues.numberOfGroups;
    this.searchResults = defaultValues.searchResults;
    this.numberOfResults = defaultValues.numberOfResults;
  };

  *fetchGroupsNextPage() {
    if (!this.hasMoreGroups && this.isLoadingFirstPage) return;
    const response = yield this.fetchGroups({ offset: this.groups.length });
    this.groups = this.groups.concat(response.groupsList);
    this.numberOfGroups = response.totalNumberOfGroups;
  }

  *fetchSearchResults() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.groupUiState;
    if (!hasFiltersApplied && !hasCustomSorting) return;

    this.searchResults = [];
    this.numberOfResults = 0;
    const response = yield this.fetchGroups({ offset: 0 });
    this.searchResults = response.groupsList;
    this.numberOfResults = response.totalNumberOfGroups;
    return this.searchResults;
  }

  *fetchSearchResultsNextPage() {
    if (!this.hasMoreResults) return;
    const response = yield this.fetchGroups({ offset: this.searchResults.length });
    const { updateFetchedResults } = this.rootStore.uiState.groupUiState;
    this.numberOfResults = response?.totalNumberOfGroups;
    this.searchResults = this.searchResults.concat(response.groupsList);
    updateFetchedResults();
  }

  *fetchAllGroups() {
    if (!this.hasMoreGroups) return this.groups;
    const response = yield this.fetchRecursively(this.groups);
    runInAction(() => {
      this.groups = response.groupsList;
      this.numberOfGroups = response.totalNumberOfGroups;
    });
    return this.groups;
  }

  *fetchAllFilteredGroups() {
    if (!this.hasMoreResults) return this.searchResults;
    const response = yield this.fetchRecursively(this.searchResults);
    runInAction(() => {
      this.searchResults = response.groupsList;
      this.numberOfResults = response.totalNumberOfGroups;
      const { updateFetchedResults } = this.rootStore.uiState.groupUiState;
      updateFetchedResults();
    });
    return this.searchResults;
  }

  fetchRecursively = async acc => {
    const response = await this.fetchGroups({
      offset: acc.length,
      pageSize: MAX_GROUP_PAGE_SIZE
    });
    const updatedAcc = acc.concat(response.groupsList);
    if (updatedAcc.length >= response.totalNumberOfGroups)
      return { groupsList: updatedAcc, totalNumberOfGroups: response.totalNumberOfGroups };
    return this.fetchRecursively(updatedAcc);
  };

  fetchGroups = async ({ offset = 0, pageSize = GROUP_PAGE_SIZE }) => {
    this.isFetching = true;
    try {
      const {
        nameFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.groupUiState;
      const [createdAfter, createdBefore] = dateFilter;

      const response = await this.api.groupsManager.getGroups({
        offset,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          name: nameFilter,
          createdBefore,
          createdAfter
        }
      });
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      return response || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchGroups',
        verb: 'getting',
        model: 'Groups'
      };
      runInAction(() => {
        this.rootStore.handleTransportLayerError(error, metadata);
        this.isFetching = false;
      });
    }
  };
}
