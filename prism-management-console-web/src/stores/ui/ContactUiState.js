import { makeAutoObservable, action } from 'mobx';
import _ from 'lodash';
import {
  CONTACT_SORTING_KEYS,
  CONTACT_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  textFilter: '',
  statusFilter: '',
  dateFilter: '',
  groupNameFilter: '',
  sortDirection: ascending,
  sortingBy: CONTACT_SORTING_KEYS.name,
  fetchedResults: null
};

export default class ContactUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  textFilter = defaultValues.textFilter;

  statusFilter = defaultValues.statusFilter;

  dateFilter = defaultValues.dateFilter;

  groupNameFilter = defaultValues.groupNameFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor({ triggerFetchResults }) {
    this.triggerFetchResults = triggerFetchResults;
    makeAutoObservable(this, {
      triggerBackendSearch: action.bound,
      rootStore: false
    });
  }

  get sortingKey() {
    return CONTACT_SORTING_KEYS_TRANSLATION[this.sortingBy];
  }

  get hasFiltersApplied() {
    return this.hasTextFilterApplied || this.hasDateFilterApplied || this.hasStatusFilterApplied;
  }

  get hasTextFilterApplied() {
    return Boolean(this.textFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter);
  }

  get hasStatusFilterApplied() {
    return Boolean(this.statusFilter);
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
    await this.triggerFetchResults();
    this.isSearching = false;
    this.isSorting = false;
  }, SEARCH_DELAY_MS);

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.textFilter = defaultValues.textFilter;
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
