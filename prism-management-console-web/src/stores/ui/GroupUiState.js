import { makeAutoObservable, computed, action } from 'mobx';
import _ from 'lodash';
import { filterByDateRange, filterByInclusion } from '../../helpers/filterHelpers';
import {
  GROUP_SORTING_KEYS,
  GROUP_SORTING_KEYS_TRANSLATOR,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  nameFilter: '',
  dateFilter: [],
  sortDirection: ascending,
  sortingBy: GROUP_SORTING_KEYS.name,
  fetchedResults: null
};
export default class GroupUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  nameFilter = defaultValues.nameFilter;

  dateFilter = defaultValues.dateFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  fetchedResults = defaultValues.fetchedResults;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      sortedFilteredGroups: computed({ requiresReaction: true }),
      triggerBackendSearch: action.bound,
      applyFilters: false,
      applySorting: false,
      sortingIsCaseSensitive: false,
      rootStore: false
    });
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

  get displayedGroups() {
    const { groups } = this.rootStore.prismStore.groupStore;
    return this.hasFiltersApplied || this.hasCustomSorting ? this.sortedFilteredGroups : groups;
  }

  get sortedFilteredGroups() {
    if (this.fetchedResults) return this.fetchedResults;
    const { groups, searchResults } = this.rootStore.prismStore.groupStore;
    const allFetchedGroups = groups.concat(searchResults);
    const groupsToFilter = _.uniqBy(allFetchedGroups, g => g.name);
    const unsortedFilteredGroups = this.applyFilters(groupsToFilter);
    const sortedFilteredGroups = this.applySorting(unsortedFilteredGroups);
    return sortedFilteredGroups;
  }

  triggerSearch = () => {
    this.isSearching = this.hasFiltersApplied;
    this.isSorting = this.hasCustomSorting;
    this.fetchedResults = null;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    const { fetchSearchResults } = this.rootStore.prismStore.groupStore;
    await fetchSearchResults();
    this.updateFetchedResults();
  }, SEARCH_DELAY_MS);

  updateFetchedResults = () => {
    const { searchResults } = this.rootStore.prismStore.groupStore;
    this.fetchedResults = searchResults;
    this.isSearching = false;
    this.isSorting = false;
  };

  applyFilters = groups =>
    groups.filter(item => {
      const matchName = !this.hasNameFilterApplied || filterByInclusion(this.nameFilter, item.name);
      const matchDate =
        !this.hasDateFilterApplied || filterByDateRange(this.dateFilter, item.createdAt);

      return matchName && matchDate;
    });

  applySorting = groups =>
    _.orderBy(
      groups,
      [
        o => (this.sortingIsCaseSensitive() ? o[this.sortingKey].toLowerCase() : o[this.sortingKey])
      ],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === GROUP_SORTING_KEYS.name;

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.nameFilter = defaultValues.nameFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
    this.fetchedResults = defaultValues.fetchedResults;
  };

  setFilterValue = (key, value) => {
    this[key] = value;
  };

  toggleSortDirection = () => {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  };

  setSortingBy = value => {
    this.sortingBy = value;
  };
}
