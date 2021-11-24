import _ from 'lodash';
import { makeAutoObservable, reaction, runInAction } from 'mobx';
import {
  GROUP_PAGE_SIZE,
  GROUP_SORTING_KEYS,
  GROUP_SORTING_KEYS_TRANSLATOR,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const fallback = {
  groups: [],
  totalNumberOfGroups: undefined
};

const defaultValues = {
  groups: [],
  totalNumberOfGroups: undefined,
  isFetching: false,
  isSearching: false,
  nameFilter: '',
  dateFilter: '',
  sortDirection: ascending,
  sortingBy: GROUP_SORTING_KEYS.name
};

export default class GroupsBaseStore {
  groups = defaultValues.groups;

  totalNumberOfGroups = defaultValues.totalNumberOfGroups;

  isFetching = defaultValues.isFetching;

  isSearching = defaultValues.isSearching;

  nameFilter = defaultValues.nameFilter;

  dateFilter = defaultValues.dateFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(api, sessionState) {
    this.api = api;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(
      this,
      {
        api: false,
        transportLayerErrorHandler: false,
        storeName: false
      },
      { autoBind: true }
    );

    reaction(() => this.nameFilter, () => this.triggerDebouncedSearch());
    reaction(() => this.dateFilter, () => this.triggerSearch());
    reaction(() => this.sortDirection, () => this.triggerSearch());
    reaction(() => this.sortingBy, () => this.triggerSearch());
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.totalNumberOfGroups === undefined;
  }

  get hasMore() {
    return this.totalNumberOfGroups > this.groups.length;
  }

  initGroupStore = () => {
    this.resetGroupsAndFilters();
    this.fetchMoreData({ startFromTheTop: true });
  };

  resetGroups = () => {
    this.groups = defaultValues.groups;
    this.totalNumberOfGroups = defaultValues.totalNumberOfGroups;
  };

  resetGroupsAndFilters() {
    this.resetGroups();
    this.isFetching = defaultValues.isFetching;
    this.isSearching = defaultValues.isSearching;
    this.nameFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
  }

  get sortingKey() {
    return GROUP_SORTING_KEYS_TRANSLATOR[this.sortingBy];
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasDateFilterApplied;
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter?.every(Boolean));
  }

  get hasCustomSorting() {
    return (
      this.sortingBy !== defaultValues.sortingBy ||
      this.sortDirection !== defaultValues.sortDirection
    );
  }

  setFilterValue(key, value) {
    // TODO: check if filter is valid?
    this[key] = value;
  }

  get filterSortingProps() {
    const { sortDirection, setSortingBy, setFilterValue, toggleSortDirection } = this;
    return {
      sortDirection,
      setSortingBy,
      setFilterValue,
      toggleSortDirection
    };
  }

  toggleSortDirection = () => {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  };

  setSortingBy = value => {
    this.sortingBy = value;
  };

  async triggerSearch() {
    this.isSearching = true;
    await this.fetchMoreData({ startFromTheTop: true });
    this.isSearching = false;
  }

  triggerDebouncedSearch() {
    this.isSearching = true;
    this.debouncedFetchSearchResults();
  }

  debouncedFetchSearchResults = _.debounce(async () => {
    await this.fetchMoreData({ startFromTheTop: true });
    runInAction(() => {
      this.isSearching = false;
    });
  }, SEARCH_DELAY_MS);

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

  *fetchMoreData({ startFromTheTop, pageSize } = {}) {
    if (!startFromTheTop && !this.hasMore) return;

    const response = yield this.fetchGroups({
      offset: startFromTheTop ? 0 : this.groups.length,
      pageSize
    });
    this.groups = startFromTheTop ? response.groupsList : this.groups.concat(response.groupsList);
    this.totalNumberOfGroups = response.totalNumberOfGroups;
  }

  refreshGroups() {
    return this.fetchMoreData({ startFromTheTop: true, pageSize: this.groups.length });
  }
}
