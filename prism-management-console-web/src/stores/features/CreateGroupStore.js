import { makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';
import Logger from '../../helpers/Logger';
import ContactsSelectStore from '../domain/ContactsSelectStore';

const defaultValues = {
  isLoadingContacts: false,
  isSaving: false
};

export default class CreateGroupStore {
  isLoadingContacts = defaultValues.isLoadingContacts;

  isSaving = defaultValues.isSaving;

  constructor(api, sessionState) {
    this.api = api;
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);
    this.contactsSelectStore = new ContactsSelectStore(api);

    makeAutoObservable(
      this,
      {
        api: false,
        contactsBaseStore: false,
        contactsSelectStore: false
      },
      { autoBind: true }
    );
  }

  get contacts() {
    return this.contactsBaseStore.contacts;
  }

  get hasMoreContacts() {
    return this.contactsBaseStore.hasMore;
  }

  get hasFiltersApplied() {
    return this.contactsBaseStore.hasFiltersApplied;
  }

  get filterSortingProps() {
    return this.contactsBaseStore.filterSortingProps;
  }

  get filterValues() {
    return { textFilter: this.contactsBaseStore.textFilter };
  }

  get isSearching() {
    return this.contactsBaseStore.isSearching;
  }

  get isFetching() {
    return this.contactsBaseStore.isFetching;
  }

  get isFetchingMore() {
    return this.contactsBaseStore.isFetchingMore;
  }

  async init() {
    this.isLoadingContacts = true;
    await this.contactsBaseStore.initContactStore();
    this.contactsSelectStore.resetSelection();
    this.isLoadingContacts = false;
  }

  fetchMoreGroupContacts = () => {
    this.contactsBaseStore.fetchMoreData();
  };

  *createGroup({ name, members, onSuccess, onError }) {
    this.isSaving = true;

    try {
      // TODO: better to do atomically on backend
      const newGroup = yield this.api.groupsManager.createGroup(name);

      if (members) {
        yield this.api.groupsManager.updateGroup(newGroup.id, { contactIdsToAdd: members });
      }

      onSuccess?.();
    } catch (error) {
      Logger.error('[CreateGroupStore.createGroup] Error: ', error);
      onError?.();
    } finally {
      this.isSaving = false;
    }
  }

  // SELECT ALL

  get selectedContacts() {
    return this.contactsSelectStore.selectedContacts;
  }

  get isLoadingSelection() {
    return this.contactsSelectStore.isLoadingSelection;
  }

  get selectAllCheckboxStateProps() {
    return this.contactsSelectStore.selectAllCheckboxStateProps;
  }

  selectAllContacts(ev, searchText) {
    return this.contactsSelectStore.selectAllContacts(ev, { searchText });
  }

  resetSelection() {
    return this.contactsSelectStore.resetSelection();
  }

  handleCherryPickSelection(record, selected) {
    return this.contactsSelectStore.handleCherryPickSelection(record, selected);
  }
}
