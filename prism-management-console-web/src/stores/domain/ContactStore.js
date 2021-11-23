import { makeAutoObservable, flow, reaction } from 'mobx';
import { CONTACT_PAGE_SIZE, MAX_CONTACT_PAGE_SIZE } from '../../helpers/constants';
import ContactUiState from '../ui/ContactUiState';

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

  constructor(api, sessionState) {
    this.api = api;
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
    // has to be declared after `fetchSearchResults` has been bound.
    // otherwise binding can be forced by passing this.fetchSearchResults.bind(this)
    this.contactUiState = new ContactUiState({ triggerFetchResults: this.fetchSearchResults });
    reaction(() => this.contactUiState.textFilter, () => this.contactUiState.triggerSearch());
    reaction(() => this.contactUiState.statusFilter, () => this.contactUiState.triggerSearch());
    reaction(() => this.contactUiState.dateFilter, () => this.contactUiState.triggerSearch());
    reaction(() => this.contactUiState.sortDirection, () => this.contactUiState.triggerSearch());
    reaction(() => this.contactUiState.sortingBy, () => this.contactUiState.triggerSearch());
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.scrollId === undefined;
  }

  get hasMore() {
    return this.scrollId;
  }

  initContactStore = () => {
    this.resetContacts();
    this.fetchMoreData({ isInitialLoading: true });
    this.resetUiState();
  };

  resetUiState = () => {
    this.contactUiState.resetState();
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
    const response = yield this.fetchRecursively(this.contacts, this.scrollId, groupName);
    return response.contactsList;
  }

  *getContactsToSelect() {
    const alreadyFetched = this.contacts;
    const currentScrollId = this.scrollId;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively(alreadyFetched, currentScrollId);
    this.contacts = response.contactsList;
    this.scrollId = '';

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
      } = this.contactUiState;

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
