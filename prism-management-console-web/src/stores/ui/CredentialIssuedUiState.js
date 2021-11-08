import { makeAutoObservable, computed, action } from 'mobx';
import _ from 'lodash';
import {
  filterByDateRange,
  filterByExactMatch,
  filterByMultipleKeys
} from '../../helpers/filterHelpers';
import {
  CREDENTIAL_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;
const {
  CREATED_ON,
  CONTACT_NAME,
  EXTERNAL_ID,
  CREDENTIAL_TYPE,
  DATE_SIGNED
} = CREDENTIAL_SORTING_KEYS_TRANSLATION;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  nameFilter: '',
  credentialTypeFilter: '',
  credentialStatusFilter: '',
  connectionStatusFilter: '',
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
      sortedFilteredCredentials: computed({ requiresReaction: true }),
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
          CONTACT_NAME,
          EXTERNAL_ID
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

  getCredentialValue = (credential, sortingKey) => {
    // CREATED_ON filter doesn't work on the backend, nor it's attribute is provided.
    // this temporary solution reverses the default sorting from the backend when
    // sorting by the CREATED_ON key and descending direction
    if (sortingKey === CREATED_ON) return credential.index;
    if (sortingKey === CREDENTIAL_TYPE) return credential.credentialData.credentialTypeDetails.id;
    if (sortingKey === DATE_SIGNED) return credential.publicationStoredAt?.seconds;
    return credential[sortingKey] || credential.credentialData[this.sortingKey];
  };

  applySorting = credentials =>
    _.orderBy(
      credentials.map((c, index) => ({ ...c, index })),
      [
        credential => {
          const value = this.getCredentialValue(credential, this.sortingKey);
          return this.sortingIsCaseSensitive() ? value.toLowerCase() : value;
        }
      ],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === CONTACT_NAME;

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
