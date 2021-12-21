import { message } from 'antd';
import i18n from 'i18next';
import _ from 'lodash';
import { makeAutoObservable, reaction, runInAction } from 'mobx';
import {
  CREDENTIAL_PAGE_SIZE,
  CREDENTIAL_SORTING_KEYS,
  CREDENTIAL_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;
const { CONTACT_NAME, EXTERNAL_ID, DATE_SIGNED } = CREDENTIAL_SORTING_KEYS_TRANSLATION;
const unsupportedSorting = [CONTACT_NAME, EXTERNAL_ID, DATE_SIGNED];

const defaultValues = {
  credentials: [],
  hasMore: true,
  isFetching: false,
  isSearching: false,
  isRefreshing: false,
  textFilter: '',
  credentialStatusFilter: undefined,
  connectionStatusFilter: undefined,
  dateFilter: undefined,
  credentialTypeFilter: undefined,
  contactIdFilter: undefined,
  sortDirection: ascending,
  sortingBy: undefined
};

const fallback = {
  credentialsList: []
};

export default class CredentialsIssuedBaseStore {
  credentials = defaultValues.credentials;

  hasMore = defaultValues.hasMore;

  isFetching = defaultValues.isFetching;

  isSearching = defaultValues.isSearching;

  isRefreshing = defaultValues.isRefreshing;

  textFilter = defaultValues.textFilter;

  credentialStatusFilter = defaultValues.credentialStatusFilter;

  connectionStatusFilter = defaultValues.connectionStatusFilter;

  dateFilter = defaultValues.dateFilter;

  credentialTypeFilter = defaultValues.credentialTypeFilter;

  contactIdFilter = defaultValues.contactIdFilter;

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
        transportLayerErrorHandler: false
      },
      {
        autoBind: true
      }
    );

    reaction(() => this.textFilter, this.triggerDebouncedSearch);
    reaction(() => this.credentialStatusFilter, this.triggerSearch);
    reaction(() => this.connectionStatusFilter, this.triggerSearch);
    reaction(() => this.credentialTypeFilter, this.triggerSearch);
    reaction(() => this.dateFilter, this.triggerSearch);
    reaction(() => this.sortDirection, this.triggerSearch);
    reaction(() => this.sortingBy, this.triggerSearch);
  }

  get isLoadingFirstPage() {
    return this.isFetching && !this.credentials.length;
  }

  initCredentialStore(contactId) {
    this.resetCredentialsAndFilters();
    this.contactIdFilter = contactId;
    return this.fetchMoreData({ startFromTheTop: true });
  }

  resetCredentials() {
    this.hasMore = true;
    this.credentials = [];
  }

  resetFilters() {
    this.textFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.dateFilter;
    this.credentialStatusFilter = defaultValues.credentialStatusFilter;
    this.connectionStatusFilter = defaultValues.connectionStatusFilter;
    this.credentialTypeFilter = defaultValues.credentialTypeFilter;
    this.contactIdFilter = defaultValues.contactIdFilter;
  }

  resetSorting() {
    this.sortDirection = ascending;
    this.sortingBy = undefined;
  }

  resetCredentialsAndFilters() {
    this.isFetching = false;
    this.isSearching = false;
    this.isRefreshing = false;
    this.resetCredentials();
    this.resetFilters();
    this.resetSorting();
  }

  // ********************** //
  // FILTERS
  // ********************** //

  setFilterValue(key, value) {
    // TODO: check if filter is valid?
    this[key] = value;
  }

  get hasFiltersApplied() {
    return this.hasTextFilterApplied || this.hasAdditionalFiltersApplied;
  }

  get hasAdditionalFiltersApplied() {
    return (
      this.hasDateFilterApplied ||
      this.hasCredentiaTypeFilter ||
      this.hasCredentialStatusFilterApplied ||
      this.hasConnectionStatusFilterApplied ||
      this.hasCredentiaTypeFilterApplied
    );
  }

  get hasTextFilterApplied() {
    return Boolean(this.textFilter);
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

  get hasCredentiaTypeFilterApplied() {
    return Boolean(this.credentialTypeFilter);
  }

  get hasCustomSorting() {
    return this.sortingBy !== CREDENTIAL_SORTING_KEYS.createdOn || this.sortDirection !== ascending;
  }

  get filterSortingProps() {
    const {
      hasFiltersApplied,
      hasAdditionalFiltersApplied,
      sortDirection,
      sortingBy,
      setSortingBy,
      setFilterValue,
      resetFilters,
      textFilter,
      dateFilter,
      credentialStatusFilter,
      connectionStatusFilter,
      credentialTypeFilter,
      toggleSortDirection
    } = this;
    return {
      hasFiltersApplied,
      hasAdditionalFiltersApplied,
      sortDirection,
      sortingBy,
      setSortingBy,
      setFilterValue,
      resetFilters,
      textFilter,
      dateFilter,
      credentialStatusFilter,
      connectionStatusFilter,
      credentialTypeFilter,
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
    this.hasMore = true;
    this.handleUnsupportedFilters();
    this.handleUnsupportedSorting();
    yield this.fetchMoreData({ startFromTheTop: true });
    this.isSearching = false;
  }

  handleUnsupportedFilters() {
    const unsupportedFilters = {
      textFilter: this.textFilter,
      credentialStatusFilter: this.credentialStatusFilter,
      connectionStatusFilter: this.connectionStatusFilter
    };

    Object.keys(unsupportedFilters)
      .filter(key => Boolean(unsupportedFilters[key]))
      .map(key =>
        message.warn(
          i18n.t('errors.filtersNotSupported', { key: i18n.t(`credentials.filters.${key}`) })
        )
      );
  }

  handleUnsupportedSorting() {
    if (unsupportedSorting.includes(this.sortingBy)) {
      message.warn(
        i18n.t('errors.sortingNotSupported', {
          key: i18n.t(`credentials.table.columns.${this.sortingBy}`)
        })
      );
    }
  }

  triggerDebouncedSearch() {
    this.isSearching = true;
    this.hasMore = true;
    this.debouncedFetchSearchResults();
  }

  debouncedFetchSearchResults = _.debounce(async () => {
    this.handleUnsupportedFilters();
    this.handleUnsupportedSorting();
    await this.fetchMoreData({ startFromTheTop: true });
    runInAction(() => {
      this.isSearching = false;
    });
  }, SEARCH_DELAY_MS);

  // ********************** //
  // DATA AND FETCHING
  // ********************** //

  *fetchCredentials({ offset = 0, pageSize = CREDENTIAL_PAGE_SIZE } = {}) {
    this.isFetching = true;
    try {
      const {
        // TODO: implement missing filters on the backend
        // nameFilter,
        // credentialStatusFilter,
        // connectionStatusFilter,
        credentialTypeFilter,
        dateFilter,
        sortDirection,
        sortingBy
      } = this;
      const response = yield this.api.credentialsManager.getCredentials({
        offset,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          credentialType: credentialTypeFilter,
          date: dateFilter
        }
      });
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.isFetching = false;
      this.updateHasMoreState(response.credentialsList, pageSize);
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCredentials',
        verb: 'getting',
        model: 'Credentials'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }

  updateHasMoreState(credentialsList, pageSize) {
    if (credentialsList.length < pageSize) {
      this.hasMore = false;
    }
  }

  // Controls credentials fetching
  *fetchMoreData({ startFromTheTop, pageSize } = {}) {
    if (this.isFetching) return; // prevents fetching same data multiple times
    if (!startFromTheTop && !this.hasMore) return;

    const response = yield this.fetchCredentials({
      offset: startFromTheTop ? 0 : this.credentials.length,
      pageSize
    });
    this.credentials = startFromTheTop
      ? response.credentialsList
      : this.credentials.concat(response.credentialsList);
  }

  *refreshCredentials() {
    this.hasMore = true;
    this.isRefreshing = true;
    try {
      yield this.fetchMoreData({
        startFromTheTop: true,
        pageSize: this.credentials.length
      });
    } finally {
      this.isRefreshing = false;
    }
  }

  *fetchAllCredentials() {
    try {
      const {
        // TODO: implement missing filters on the backend
        // nameFilter,
        // credentialStatusFilter,
        // connectionStatusFilter,
        credentialTypeFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this;
      const allCredentials = yield this.api.credentialsManager.getAllCredentials({
        limit: null,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          credentialType: credentialTypeFilter,
          date: dateFilter
        }
      });
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.isFetching = false;
      return allCredentials;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchAllCredentials',
        verb: 'getting',
        model: 'Credentials'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
      return fallback.credentialsList;
    }
  }
}
