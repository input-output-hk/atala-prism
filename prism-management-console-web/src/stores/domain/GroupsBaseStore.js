import _ from 'lodash';
import { makeAutoObservable, reaction, runInAction } from 'mobx';
import {
  GROUP_PAGE_SIZE,
  GROUP_SORTING_KEYS,
  GROUP_SORTING_KEYS_TRANSLATOR,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const fallback = {
  groupsList: [],
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

  init() {
    this.resetGroupsAndFilters();
    return this.fetchMoreData({ startFromTheTop: true });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.totalNumberOfGroups === undefined;
  }

  get hasMore() {
    return this.totalNumberOfGroups > this.groups.length;
  }

  get isFetchingMore() {
    return this.isFetching && this.totalNumberOfGroups && !this.isSearching;
  }

  resetGroups() {
    this.groups = defaultValues.groups;
    this.totalNumberOfGroups = defaultValues.totalNumberOfGroups;
  }

  resetFilters() {
    this.nameFilter = defaultValues.nameFilter;
    this.dateFilter = defaultValues.dateFilter;
  }

  resetSorting() {
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
  }

  resetGroupsAndFilters() {
    this.isFetching = defaultValues.isFetching;
    this.isSearching = defaultValues.isSearching;
    this.resetGroups();
    this.resetFilters();
    this.resetSorting();
  }

  get sortingKey() {
    return GROUP_SORTING_KEYS_TRANSLATOR[this.sortingBy];
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAdditionalFiltersApplied;
  }

  get hasAdditionalFiltersApplied() {
    return this.hasDateFilterApplied;
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter);
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
    const {
      nameFilter,
      dateFilter,
      hasAdditionalFiltersApplied,
      sortDirection,
      setSortingBy,
      setFilterValue,
      resetFilters,
      toggleSortDirection
    } = this;
    return {
      nameFilter,
      dateFilter,
      hasAdditionalFiltersApplied,
      sortDirection,
      setSortingBy,
      setFilterValue,
      resetFilters,
      toggleSortDirection
    };
  }

  toggleSortDirection() {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  }

  setSortingBy(value) {
    this.sortingBy = value;
  }

  *triggerSearch() {
    this.isSearching = true;
    yield this.fetchMoreData({ startFromTheTop: true });
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
      const { nameFilter, dateFilter, sortDirection, sortingBy } = this;
      const createdAfter = dateFilter;
      const createdBefore = dateFilter;

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

      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.isFetching = false;
      return response || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchGroups',
        verb: 'getting',
        model: 'Groups'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }

  *fetchMoreData({ startFromTheTop, pageSize } = {}) {
    if (this.isFetching) return;
    if (!startFromTheTop && !this.hasMore) return;

    const response = yield this.fetchGroups({
      offset: startFromTheTop ? 0 : this.groups.length,
      pageSize
    });
    this.groups = startFromTheTop ? response.groupsList : this.groups.concat(response.groupsList);
    this.totalNumberOfGroups = response.totalNumberOfGroups;
  }

  /**
   *
   * @param refreshCountDiff - when a group is deleted or copied, we have to increase/decrease the
   * number of refreshed rows
   */
  refreshGroups({ refreshCountDiff = 0 } = {}) {
    return this.fetchMoreData({
      startFromTheTop: true,
      pageSize: this.groups.length + refreshCountDiff
    });
  }
}
