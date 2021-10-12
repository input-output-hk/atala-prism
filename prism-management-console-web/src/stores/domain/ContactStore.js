import { makeAutoObservable, observable, flow, computed, action, runInAction } from 'mobx';
import { GROUP_PAGE_SIZE, MAX_GROUP_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isFetching: false,
  contacts: [],
  numberOfContacts: undefined,
  searchResults: [],
  numberOfResults: undefined
};

const fallback = {
  contactsList: [],
  totalNumberOfContacts: undefined
};
export default class ContactStore {
  isFetching = defaultValues.isFetching;

  contacts = defaultValues.contacts;

  numberOfContacts = defaultValues.numberOfContacts;

  searchResults = defaultValues.searchResults;

  numberOfResults = defaultValues.numberOfResults;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      isFetching: observable,
      contacts: observable,
      searchResults: observable,
      isLoadingFirstPage: computed,
      hasMore: computed,
      hasMoreContacts: computed,
      hasMoreResults: computed,
      fetchMoreData: computed,
      resetContacts: action,
      fetchContactsNextPage: flow.bound,
      fetchSearchResults: flow.bound,
      fetchSearchResultsNextPage: flow.bound,
      updateFetchedResults: action,
      fetchAllContacts: flow.bound,
      fetchAllFilteredContacts: flow.bound,
      fetchContacts: action,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.numberOfContacts === undefined;
  }

  get hasMore() {
    const { hasFiltersApplied } = this.rootStore.uiState.groupUiState;
    return hasFiltersApplied ? this.hasMoreResults : this.hasMoreContacts;
  }

  get hasMoreContacts() {
    return this.numberOfContacts > this.contacts.length;
  }

  get hasMoreResults() {
    return this.numberOfResults > this.searchResults.length;
  }

  get fetchMoreData() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.groupUiState;
    return hasFiltersApplied || hasCustomSorting
      ? this.fetchSearchResultsNextPage
      : this.fetchContactsNextPage;
  }

  resetContacts = () => {
    this.isFetching = defaultValues.isFetching;
    this.contacts = defaultValues.contacts;
    this.numberOfContacts = defaultValues.numberOfContacts;
    this.searchResults = defaultValues.searchResults;
    this.numberOfResults = defaultValues.numberOfResults;
  };

  *fetchContactsNextPage() {
    if (!this.hasMoreContacts && this.isLoadingFirstPage) return;
    const response = yield this.fetchContacts({ offset: this.contacts.length });
    this.contacts = this.contacts.concat(response.contactsList);
    this.numberOfContacts = response.totalNumberOfContacts;
  }

  *fetchSearchResults() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.groupUiState;
    if (!hasFiltersApplied && !hasCustomSorting) return;

    this.searchResults = [];
    this.numberOfResults = 0;
    const response = yield this.fetchContacts({ offset: 0 });
    this.searchResults = response.contactsList;
    this.numberOfResults = response.totalNumberOfContacts;
    return this.searchResults;
  }

  *fetchSearchResultsNextPage() {
    if (!this.hasMoreResults) return;
    const response = yield this.fetchContacts({ offset: this.searchResults.length });
    const { updateFetchedResults } = this.rootStore.uiState.groupUiState;
    this.numberOfResults = response?.totalNumberOfContacts;
    this.searchResults = this.searchResults.concat(response.contactsList);
    updateFetchedResults();
  }

  *fetchAllContacts() {
    if (!this.hasMoreContacts) return this.contacts;
    let contactsAcc = this.contacts;
    const fetchRecursively = async () => {
      const response = await this.fetchContacts({
        offset: contactsAcc.length,
        pageSize: MAX_GROUP_PAGE_SIZE
      });
      contactsAcc = contactsAcc.concat(response.contactsList);
      if (contactsAcc.length >= response.totalNumberOfContacts)
        return { contactsList: contactsAcc, totalNumberOfContacts: response.totalNumberOfContacts };
      return fetchRecursively();
    };

    const response = yield fetchRecursively();
    runInAction(() => {
      this.contacts = response.contactsList;
      this.numberOfContacts = response.totalNumberOfContacts;
    });
    return this.contacts;
  }

  *fetchAllFilteredContacts() {
    if (!this.hasMoreResults) return this.searchResults;
    let contactsAcc = this.searchResults;
    const fetchRecursively = async () => {
      const response = await this.fetchContacts({
        offset: contactsAcc.length,
        pageSize: MAX_GROUP_PAGE_SIZE
      });
      contactsAcc = contactsAcc.concat(response.contactsList);
      if (contactsAcc.length >= response.totalNumberOfContacts)
        return { contactsList: contactsAcc, totalNumberOfContacts: response.totalNumberOfContacts };
      return fetchRecursively();
    };

    const response = yield fetchRecursively();
    runInAction(() => {
      this.searchResults = response.contactsList;
      this.numberOfResults = response.totalNumberOfContacts;
      const { updateFetchedResults } = this.rootStore.uiState.groupUiState;
      updateFetchedResults();
    });
    return this.searchResults;
  }

  fetchContacts = async ({ offset = 0, pageSize = GROUP_PAGE_SIZE }) => {
    this.isFetching = true;
    try {
      const {
        nameFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.groupUiState;
      const [createdAfter, createdBefore] = dateFilter;

      const response = await this.api.contactsManager.getContacts({
        offset,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          name: nameFilter,
          createdBefore,
          createdAfter
        }
      });
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      return response || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchContacts',
        verb: 'getting',
        model: 'Contacts'
      };
      runInAction(() => {
        this.rootStore.handleTransportLayerError(error, metadata);
        this.isFetching = false;
      });
    }
  };
}
