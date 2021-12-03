import { makeAutoObservable, reaction } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';
import ContactsSelectStore from '../domain/ContactsSelectStore';
import GroupsSelectStore from '../domain/GroupsSelectStore';
import GroupsBaseStore from '../domain/GroupsBaseStore';

export default class CreateCredentialPageStore {
  isInitRecipientsInProgress = false;

  constructor(api, sessionState) {
    this.api = api;
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);
    this.groupsBaseStore = new GroupsBaseStore(api, sessionState);
    this.contactsSelectStore = new ContactsSelectStore(api);
    this.groupsSelectStore = new GroupsSelectStore(api);

    makeAutoObservable(
      this,
      {
        api: false,
        contactsBaseStore: false,
        groupsBaseStore: false,
        contactsSelectStore: false,
        groupsSelectStore: false
      },
      {
        autoBind: true
      }
    );

    reaction(() => this.contactsBaseStore.textFilter, this.resetContactsSelection);
    reaction(() => this.groupsBaseStore.nameFilter, this.resetGroupsSelection);
  }

  initRecipients = async () => {
    this.isInitRecipientsInProgress = true;
    this.contactsSelectStore.resetSelection();
    this.groupsSelectStore.resetSelection();
    await Promise.all([this.contactsBaseStore.initContactStore(), this.groupsBaseStore.init()]);
    this.isInitRecipientsInProgress = false;
  };

  // contacts

  get contacts() {
    return this.contactsBaseStore.contacts;
  }

  get hasMoreContacts() {
    return this.contactsBaseStore.hasMore;
  }

  get hasContactsFiltersApplied() {
    return this.contactsBaseStore.hasFiltersApplied;
  }

  get contactsFilterSortingProps() {
    return this.contactsBaseStore.filterSortingProps;
  }

  get isSearchingContacts() {
    return this.contactsBaseStore.isSearching;
  }

  get isFetchingMoreContacts() {
    return this.contactsBaseStore.isFetchingMore;
  }

  fetchMoreContacts() {
    return this.contactsBaseStore.fetchMoreData();
  }

  // contacts select all

  get selectedContacts() {
    return this.contactsSelectStore.selectedContacts;
  }

  get selectedContactsObjects() {
    return this.contactsSelectStore.selectedContactsObjects;
  }

  get isLoadingContactsSelection() {
    return this.contactsSelectStore.isLoadingSelection;
  }

  get contactsSelectAllCheckboxStateProps() {
    return this.contactsSelectStore.selectAllCheckboxStateProps;
  }

  selectAllContacts(ev) {
    return this.contactsSelectStore.selectAllContacts(ev, {
      searchText: this.contactsBaseStore.textFilter
    });
  }

  resetContactsSelection() {
    return this.contactsSelectStore.resetSelection();
  }

  handleContactsCherryPickSelection(record, selected) {
    return this.contactsSelectStore.handleCherryPickSelection(record, selected);
  }

  // groups

  get groups() {
    return this.groupsBaseStore.groups;
  }

  get hasMoreGroups() {
    return this.groupsBaseStore.hasMore;
  }

  get hasGroupsFiltersApplied() {
    return this.groupsBaseStore.hasFiltersApplied;
  }

  get groupsFilterSortingProps() {
    return this.groupsBaseStore.filterSortingProps;
  }

  get isSearchingGroups() {
    return this.groupsBaseStore.isSearching;
  }

  get isFetchingMoreGroups() {
    return this.groupsBaseStore.isFetchingMore;
  }

  fetchMoreGroups() {
    return this.groupsBaseStore.fetchMoreData();
  }

  // groups select all

  get selectedGroupsObjects() {
    return this.groupsSelectStore.selectedGroupsObjects;
  }

  get selectedGroups() {
    return this.groupsSelectStore.selectedGroups;
  }

  get selectedGroupsNames() {
    return this.groupsSelectStore.selectedGroupsNames;
  }

  get isLoadingGroupsSelection() {
    return this.groupsSelectStore.isLoadingSelection;
  }

  get groupsSelectAllCheckboxStateProps() {
    return this.groupsSelectStore.selectAllCheckboxStateProps;
  }

  selectAllGroups(ev) {
    return this.groupsSelectStore.selectAllGroups(ev, {
      name: this.groupsBaseStore.nameFilter
    });
  }

  resetGroupsSelection() {
    return this.groupsSelectStore.resetSelection();
  }

  handleGroupsCherryPickSelection(record, selected) {
    return this.groupsSelectStore.handleCherryPickSelection(record, selected);
  }
}
