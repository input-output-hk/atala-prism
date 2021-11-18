import { flow, makeAutoObservable, reaction } from 'mobx';
import {
  CONTACT_PAGE_SIZE,
  CONTACT_SORTING_KEYS,
  CONTACT_SORTING_KEYS_TRANSLATION,
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

  // TODO: can we use isFetching instead of isSorting and isSearching
  isFetching = defaultValues.isFetching;

  /// FROM UI STORE
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

    makeAutoObservable(this, {
      fetchContacts: flow.bound,
      fetchMoreData: flow.bound,
      fetchSearchResults: flow.bound,
      api: false,
      transportLayerErrorHandler: false
    });

    reaction(() => this.textFilter, () => this.fetchSearchResults());
    reaction(() => this.statusFilter, () => this.fetchSearchResults());
    reaction(() => this.dateFilter, () => this.fetchSearchResults());
    reaction(() => this.sortDirection, () => this.fetchSearchResults());
    reaction(() => this.sortingBy, () => this.fetchSearchResults());
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.scrollId === undefined;
  }

  get hasMore() {
    return this.scrollId;
  }

  initContactStore = groupName => {
    this.resetContactsAndFilters();
    this.groupNameFilter = groupName;
    return this.fetchMoreData({ isInitialLoading: true });
  };

  resetContacts = () => {
    this.contacts = defaultValues.contacts;
    this.scrollId = defaultValues.scrollId;
  };

  resetContactsAndFilters = () => {
    this.resetContacts();
    this.isSearching = defaultValues.isSearching;
    this.textFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
  };

  // ********************** //
  // FILTERS
  // ********************** //

  setFilterValue = (key, value) => {
    // TODO: check if filter is valid?
    this[key] = value;
  };

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

  toggleSortDirection = () => {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  };

  setSortingBy = value => {
    this.sortingBy = value;
  };

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

  // Initial load and load more
  *fetchMoreData({ isInitialLoading } = {}) {
    if (!isInitialLoading && !this.hasMore) return;

    const response = yield this.fetchContacts({
      scrollId: !isInitialLoading && this.scrollId
    });
    this.contacts = isInitialLoading
      ? response.contactsList
      : this.contacts.concat(response.contactsList);
    this.scrollId = response.newScrollId;
  }

  *fetchSearchResults() {
    // this is just used to trigger initial search, later load more is performed in fetchMoreData

    // TODO: let it stay like this, I would rather find a way to unify search
    //  and load more, but it's better to wait for it until we finish 1 phase of refactoring.
    //  Maybe this is good as it is, just to add more comments, but let's review later
    const response = yield this.fetchContacts({ scrollId: null });
    this.resetContacts();
    this.contacts = this.contacts.concat(response.contactsList);
    this.scrollId = response.newScrollId;
  }

  // TODO: implement
  refreshContacts = () => this.fetchSearchResults();
}
