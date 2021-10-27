import { makeAutoObservable, observable, computed, action } from 'mobx';
import _ from 'lodash';
import {
  filterByDateRange,
  filterByExactMatch,
  filterByMultipleKeys
} from '../../helpers/filterHelpers';
import {
  CREDENTIAL_SORTING_KEYS,
  CREDENTIAL_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  nameFilter: '',
  credentialTypeFilter: '',
  credentialStatusFilter: '',
  connectionStatusFilter: '',
  dateFilter: null,
  sortDirection: ascending,
  sortingBy: CREDENTIAL_SORTING_KEYS.createdOn
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
      isSearching: observable,
      isSorting: observable,
      nameFilter: observable,
      credentialTypeFilter: observable,
      credentialStatusFilter: observable,
      connectionStatusFilter: observable,
      dateFilter: observable,
      sortDirection: observable,
      sortingBy: observable,
      sortingKey: computed,
      hasFiltersApplied: computed,
      hasNameFilterApplied: computed,
      hasCredentialStatusFilterApplied: computed,
      hasConnectionStatusFilterApplied: computed,
      hasDateFilterApplied: computed,
      hasCustomSorting: computed,
      displayedCredentials: computed,
      sortedFilteredCredentials: computed({ requiresReaction: true }),
      triggerSearch: action,
      triggerBackendSearch: action.bound,
      applyFilters: action,
      applySorting: action,
      sortingIsCaseSensitive: false,
      resetState: action,
      setFilterValue: action,
      toggleSortDirection: action,
      setSortingBy: action,
      rootStore: false
    });
  }

  get sortingKey() {
    return CREDENTIAL_SORTING_KEYS_TRANSLATION[this.sortingBy];
  }

  get hasFiltersApplied() {
    return (
      this.hasNameFilterApplied ||
      this.hasDateFilterApplied ||
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

  get hasCustomSorting() {
    return (
      this.sortingBy !== defaultValues.sortingBy ||
      this.sortDirection !== defaultValues.sortDirection
    );
  }

  get displayedCredentials() {
    const { credentials } = this.rootStore.prismStore.credentialIssuedStore;
    return this.hasFiltersApplied || this.hasCustomSorting
      ? this.sortedFilteredCredentials
      : credentials;
  }

  get sortedFilteredCredentials() {
    const { credentials, searchResults } = this.rootStore.prismStore.credentialIssuedStore;
    const allFetchedCredentials = credentials.concat(searchResults);
    const credentialsToFilter = _.uniqBy(allFetchedCredentials, c => c.credentialId);
    const unsortedFilteredCredentials = this.applyFilters(credentialsToFilter);
    const sortedFilteredCredentials = this.applySorting(unsortedFilteredCredentials);
    return sortedFilteredCredentials;
  }

  triggerSearch = () => {
    this.isSearching = this.hasFiltersApplied;
    this.isSorting = this.hasCustomSorting;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    const { fetchSearchResults } = this.rootStore.prismStore.credentialIssuedStore;
    await fetchSearchResults();
    this.isSearching = false;
    this.isSorting = false;
  }, SEARCH_DELAY_MS);

  applyFilters = credentials =>
    credentials.filter(item => {
      const matchName =
        !this.hasNameFilterApplied ||
        filterByMultipleKeys(this.nameFilter, { ...item.contactData, ...item.credentialData }, [
          'contactName',
          'externalId'
        ]);
      const matchDate =
        !this.hasDateFilterApplied || filterByDateRange(this.dateFilter, item.publicationStoredAt);
      const matchCredentialStatus =
        !this.hasCredentialStatusFilterApplied ||
        filterByExactMatch(this.credentialStatusFilter, item.status);
      const matchConnectionStatus =
        !this.hasConnectionStatusFilter ||
        filterByExactMatch(this.connectionStatusFilter, item.contactData.connectionStatus);

      return matchName && matchDate && matchCredentialStatus && matchConnectionStatus;
    });

  applySorting = credentials =>
    _.orderBy(
      credentials,
      [
        credential =>
          this.sortingIsCaseSensitive()
            ? credential[this.sortingKey].toLowerCase()
            : credential[this.sortingKey]
      ],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === 'contactName';

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
