import _ from 'lodash';
import { makeAutoObservable, reaction, runInAction } from 'mobx';
import {
  CONTACT_PAGE_SIZE,
  CONTACT_SORTING_KEYS,
  CONTACT_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const fallback = {
  contactsList: [],
  newScrollId: undefined
};

const defaultValues = {
  contacts: [],
  scrollId: undefined,
  isFetching: false,
  isSearching: false,
  textFilter: '',
  statusFilter: '',
  dateFilter: '',
  groupNameFilter: '',
  sortDirection: ascending,
  sortingBy: CONTACT_SORTING_KEYS.name
};

export default class ContactsBaseStore {
  contacts = defaultValues.contacts;

  scrollId = defaultValues.scrollId;

  isFetching = defaultValues.isFetching;

  isSearching = defaultValues.isSearching;

  textFilter = defaultValues.textFilter;

  statusFilter = defaultValues.statusFilter;

  dateFilter = defaultValues.dateFilter;

  groupNameFilter = defaultValues.groupNameFilter;

  sortDirection = ascending;

  sortingBy = CONTACT_SORTING_KEYS.name;

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

    reaction(() => this.textFilter, () => this.triggerDebouncedSearch());
    reaction(() => this.statusFilter, () => this.triggerSearch());
    reaction(() => this.dateFilter, () => this.triggerSearch());
    reaction(() => this.sortDirection, () => this.triggerSearch());
    reaction(() => this.sortingBy, () => this.triggerSearch());
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.scrollId === undefined;
  }

  get hasMore() {
    return this.scrollId;
  }

  initContactStore(groupName) {
    this.resetContactsAndFilters();
    this.groupNameFilter = groupName;
    return this.fetchMoreData({ startFromTheTop: true });
  }

  resetContacts() {
    this.contacts = defaultValues.contacts;
    this.scrollId = defaultValues.scrollId;
  }

  resetContactsAndFilters() {
    this.resetContacts();
    this.isFetching = defaultValues.isFetching;
    this.isSearching = defaultValues.isSearching;
    this.textFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
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
    await this.fetchMoreData({ startFromTheTop: true });
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

  // ********************** //
  // DATA AND FETCHING
  // ********************** //

  *fetchContacts({ scrollId, pageSize = CONTACT_PAGE_SIZE } = {}) {
    this.isFetching = true;

    try {
      const {
        groupNameFilter,
        textFilter,
        dateFilter,
        statusFilter,
        sortDirection,
        sortingBy
      } = this;

      const response = yield this.api.contactsManager.getContacts({
        scrollId,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          searchText: textFilter,
          createdAt: dateFilter,
          connectionStatus: statusFilter,
          groupName: groupNameFilter
        }
      });
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.isFetching = false;
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchContacts',
        verb: 'getting',
        model: 'Contacts'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }

  // Controls contacts fetching
  *fetchMoreData({ startFromTheTop, pageSize } = {}) {
    if (!startFromTheTop && !this.hasMore) return;

    const response = yield this.fetchContacts({
      scrollId: !startFromTheTop && this.scrollId,
      pageSize
    });
    this.contacts = startFromTheTop
      ? response.contactsList
      : this.contacts.concat(response.contactsList);
    this.scrollId = response.newScrollId;
  }

  refreshContacts() {
    return this.fetchMoreData({ startFromTheTop: true, pageSize: this.contacts.length });
  }
}
