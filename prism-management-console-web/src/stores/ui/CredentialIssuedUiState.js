import { makeAutoObservable, action } from 'mobx';
import _ from 'lodash';
import {
  CREDENTIAL_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;
const { CREATED_ON } = CREDENTIAL_SORTING_KEYS_TRANSLATION;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  nameFilter: undefined,
  credentialTypeFilter: undefined,
  credentialStatusFilter: undefined,
  connectionStatusFilter: undefined,
  dateFilter: null,
  sortDirection: ascending,
  sortingBy: CREATED_ON
};
export default class CredentialIssuedUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  nameFilter = defaultValues.nameFilter;

  credentialStatusFilter = defaultValues.credentialStatusFilter;

  connectionStatusFilter = defaultValues.connectionStatusFilter;

  dateFilter = defaultValues.dateFilter;

  credentialTypeFilter = defaultValues.credentialTypeFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      triggerBackendSearch: action.bound,
      sortingIsCaseSensitive: false,
      getCredentialValue: false,
      rootStore: false
    });
  }

  get sortingKey() {
    return this.sortingBy;
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAditionalFiltersApplied;
  }

  get hasAditionalFiltersApplied() {
    return (
      this.hasDateFilterApplied ||
      this.hasCredentiaTypeFilter ||
      this.hasCredentialStatusFilterApplied ||
      this.hasConnectionStatusFilterApplied
    );
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter);
  }

  get hasCredentialStatusFilterApplied() {
    return Boolean(this.credentialStatusFilter);
  }

  get hasConnectionStatusFilterApplied() {
    return Boolean(this.connectionStatusFilter);
  }

  get hasCredentiaTypeFilter() {
    return Boolean(this.credentialTypeFilter);
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
    const { fetchSearchResults } = this.rootStore.prismStore.credentialIssuedStore;
    await fetchSearchResults();
    this.isSearching = false;
    this.isSorting = false;
  }, SEARCH_DELAY_MS);

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.nameFilter = defaultValues.nameFilter;
    this.credentialTypeFilter = defaultValues.credentialTypeFilter;
    this.credentialStatusFilter = defaultValues.credentialStatusFilter;
    this.connectionStatusFilter = defaultValues.connectionStatusFilter;
    this.dateFilter = defaultValues.dateFilter;
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
