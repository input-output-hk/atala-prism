import { makeAutoObservable, flow } from 'mobx';
import { CONTACT_PAGE_SIZE, MAX_CONTACT_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isSaving: false,
  isFetching: false,
  contacts: [],
  scrollId: undefined
};

const fallback = {
  contactsList: [],
  newScrollId: undefined
};
export default class ContactStore {
  isSaving = defaultValues.isSaving;

  isFetching = defaultValues.isFetching;

  contacts = defaultValues.contacts;

  scrollId = defaultValues.scrollId;

  constructor(api, sessionState, rootContactStore) {
    this.api = api;
    this.rootContactStore = rootContactStore;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      fetchMoreData: flow.bound,
      fetchSearchResults: flow.bound,
      fetchAllContacts: flow.bound,
      getContactsToSelect: flow.bound,
      fetchContacts: flow.bound,
      updateContact: flow.bound,
      fetchRecursively: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.contactsScrollId === undefined;
  }

  get hasMore() {
    return this.scrollId;
  }

  initContactStore = () => {
    this.resetContacts();
    this.fetchMoreData({ isInitialLoading: true });
  };

  resetContacts = () => {
    this.contacts = defaultValues.contacts;
    this.scrollId = defaultValues.scrollId;
  };

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

  refreshContacts = () => {
    // TODO: implement
  };

  *fetchSearchResults() {
    const response = yield this.fetchContacts({ scrollId: null });
    this.resetContacts();
    this.contacts = this.contacts.concat(response.contactsList);
    this.scrollId = response.newScrollId;
  }

  *fetchAllContacts(groupName) {
    const response = yield this.fetchRecursively(this.contacts, this.contactsScrollId, groupName);
    return response.contactsList;
  }

  *getContactsToSelect() {
    const alreadyFetched = this.contacts;
    const currentScrollId = this.scrollId;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively(alreadyFetched, currentScrollId);
    this.contacts = response.contactsList;
    this.contactsScrollId = '';

    return response.contactsList;
  }

  fetchRecursively = async (acc = [], scrollId, groupName) => {
    const response = await this.fetchContacts({
      scrollId,
      pageSize: MAX_CONTACT_PAGE_SIZE,
      groupName
    });
    const updatedAcc = acc.concat(response.contactsList);
    return response.newScrollId
      ? this.fetchRecursively(updatedAcc, response.newScrollId, groupName)
      : { contactsList: updatedAcc };
  };

  *fetchContacts({ scrollId, groupName, pageSize = CONTACT_PAGE_SIZE } = {}) {
    this.isFetching = true;
    try {
      const {
        textFilter,
        dateFilter,
        statusFilter,
        sortDirection,
        sortingBy
      } = this.rootContactStore.contactUiState;

      const response = yield this.api.contactsManager.getContacts({
        scrollId,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          searchText: textFilter,
          createdAt: dateFilter,
          connectionStatus: statusFilter,
          groupName
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

  *updateContact(contactId, newContactData) {
    try {
      this.isSaving = true;
      const response = yield this.api.contactsManager.updateContact(contactId, newContactData);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.isSaving = false;
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'updateContact',
        verb: 'saving',
        model: 'Contact'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
  }
}
