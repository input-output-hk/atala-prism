import { makeAutoObservable, observable, computed, action, runInAction } from 'mobx';
import _ from 'lodash';
import { filterByExactMatch, filterByInclusion } from '../../helpers/filterHelpers';
import {
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS,
  TEMPLATES_SORTING_KEYS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  nameFilter: '',
  dateFilter: [],
  sortDirection: ascending,
  sortingBy: TEMPLATES_SORTING_KEYS.name
};

export default class GroupUiState {
  isSearching = defaultValues.isSearching;

  nameFilter = defaultValues.nameFilter;

  dateFilter = defaultValues.dateFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      nameFilter: observable,
      dateFilter: observable,
      isSearching: observable,
      filteredGroups: computed({ requiresReaction: true }),
      hasFiltersApplied: computed,
      hasNameFilterApplied: computed,
      hasAditionalFiltersApplied: computed,
      toggleSortDirection: action,
      setNameFilter: action,
      resetState: action,
      applyFilters: false,
      applySorting: false,
      sortingIsCaseSensitive: false,
      rootStore: false
    });
    this.triggerBackendSearch = this.triggerBackendSearch.bind(this);
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasAditionalFiltersApplied() {
    return Boolean(this.dateFilter);
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAditionalFiltersApplied;
  }

  get filteredGroups() {
    const { groups, searchResults } = this.rootStore.prismStore.groupStore;
    const groupsToFilter = this.isSearching ? groups : searchResults;
    const unsortedFilteredGroups = this.applyFilters(groupsToFilter);
    const sortedFilteredGroups = this.applySorting(unsortedFilteredGroups);
    return sortedFilteredGroups;
  }

  triggerSearch = () => {
    this.isSearching = true;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    const { fetchSearchResults } = this.rootStore.prismStore.groupStore;
    await fetchSearchResults();
    runInAction(() => {
      this.isSearching = false;
    })
  }, SEARCH_DELAY_MS);

  applyFilters = templates =>
    templates.filter(item => {
      const matchName = filterByInclusion(this.nameFilter, item.name);
      const matchDate = filterByExactMatch(this.dateFilter, item.lastEdited);

      return matchName && matchDate;
    });

  applySorting = templates =>
    _.orderBy(
      templates,
      [o => (this.sortingIsCaseSensitive() ? o[this.sortingBy].toLowerCase() : o[this.sortingBy])],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === TEMPLATES_SORTING_KEYS.name;

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.nameFilter = defaultValues.nameFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
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
