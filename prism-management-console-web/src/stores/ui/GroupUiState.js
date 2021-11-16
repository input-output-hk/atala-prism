import { makeAutoObservable, action } from 'mobx';
import _ from 'lodash';
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
  sortingBy: GROUP_SORTING_KEYS.name
};
export default class GroupUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  nameFilter = defaultValues.nameFilter;

  dateFilter = defaultValues.dateFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(groupStore) {
    this.groupStore = groupStore;
    makeAutoObservable(this, {
      triggerBackendSearch: action.bound,
      groupStore: false
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

  triggerSearch = () => {
    this.isSearching = true;
    this.isSorting = true;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    await this.groupStore.fetchSearchResults();
    this.isSearching = false;
    this.isSorting = false;
  }, SEARCH_DELAY_MS);

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
