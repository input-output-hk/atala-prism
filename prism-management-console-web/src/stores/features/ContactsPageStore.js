import { makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';

export default class ContactsPageStore {
  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);

    makeAutoObservable(
      this,
      {
        api: false,
        sessionState: false,
        contactsBaseStore: false
      },
      { autoBind: true }
    );
  }

  get contacts() {
    return this.contactsBaseStore.contacts;
  }

  get hasMore() {
    return this.contactsBaseStore.hasMore;
  }

  get filterSortingProps() {
    return this.contactsBaseStore.filterSortingProps;
  }

  get isSearching() {
    return this.contactsBaseStore.isSearching;
  }

  get isFetching() {
    return this.contactsBaseStore.isFetching;
  }

  get isLoadingFirstPage() {
    return this.contactsBaseStore.isLoadingFirstPage;
  }

  fetchMoreData() {
    return this.contactsBaseStore.fetchMoreData();
  }

  refreshContacts() {
    return this.contactsBaseStore.refreshContacts();
  }

  initContactsPageStore() {
    return this.contactsBaseStore.initContactStore();
  }
}
