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

const fallback = {
  credentialsList: []
};

const defaultValues = {
  credentials: [],
  isFetching: false,
  isSearching: false,
  hasMore: true,
  textFilter: '',
  statusFilter: '',
  dateFilter: '',
  contactIdFilter: '',
  sortDirection: ascending,
  sortingBy: CREDENTIAL_SORTING_KEYS.createdOn
};
export default class CredentialsIssuedBaseStore {
  credentials = defaultValues.credentials;

  hasMore = defaultValues.hasMore;

  isFetching = defaultValues.isFetching;

  isSearching = defaultValues.isSearching;

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

    reaction(() => this.textFilter, () => this.triggerDebouncedSearch());
    reaction(() => this.credentialStatusFilter, () => this.triggerSearch());
    reaction(() => this.connectionStatusFilter, () => this.triggerSearch());
    reaction(() => this.credentialTypeFilter, () => this.triggerSearch());
    reaction(() => this.dateFilter, () => this.triggerSearch());
    reaction(() => this.sortDirection, () => this.triggerSearch());
    reaction(() => this.sortingBy, () => this.triggerSearch());
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
    this.hasMore = defaultValues.hasMore;
    this.credentials = defaultValues.credentials;
  }

  resetContactsAndFilters() {
    this.resetCredentials();
    this.isFetching = defaultValues.isFetching;
    this.isSearching = defaultValues.isSearching;
    this.textFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.credentialStatusFilter = defaultValues.credentialStatusFilter;
    this.connectionStatusFilter = defaultValues.connectionStatusFilter;
    this.credentialTypeFilter = defaultValues.credentialTypeFilter;
    this.contactIdFilter = defaultValues.contactIdFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
  }

  // ********************** //
  // FILTERS
  // ********************** //

  setFilterValue(key, value) {
    // TODO: check if filter is valid?
    this[key] = value;
  }

  get hasFiltersApplied() {
    return this.hasTextFilterApplied || this.hasAditionalFiltersApplied;
  }

  get hasAditionalFiltersApplied() {
    return (
      this.hasDateFilterApplied ||
      this.hasCredentiaTypeFilter ||
      this.hasCredentialStatusFilterApplied ||
      this.hasConnectionStatusFilterApplied
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

  get hasCredentiaTypeFilter() {
    return Boolean(this.credentialTypeFilter);
  }

  get hasCustomSorting() {
    return (
      this.sortingBy !== defaultValues.sortingBy ||
      this.sortDirection !== defaultValues.sortDirection
    );
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

  toggleSortDirection() {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  }

  setSortingBy(value) {
    this.sortingBy = value;
  }

  async triggerSearch() {
    this.isSearching = true;
    this.hasMore = true;
    this.handleUnsupportedFilters();
    this.handleUnsupportedSorting();
    await this.fetchMoreData({ startFromTheTop: true });
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
        dateFilter = [],
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
    if (!startFromTheTop && !this.hasMore) return;

    const response = yield this.fetchCredentials({
      offset: startFromTheTop ? 0 : this.credentials.length,
      pageSize
    });
    this.credentials = startFromTheTop
      ? response.credentialsList
      : this.credentials.concat(response.credentialsList);
  }

  refreshCredentials() {
    this.hasMore = true;
    return this.fetchMoreData({ startFromTheTop: true, pageSize: this.credentials.length });
  }

  *fetchAllCredentials() {
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
  }
}
