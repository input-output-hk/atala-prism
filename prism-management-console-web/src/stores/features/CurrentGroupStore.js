import { makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';
import Logger from '../../helpers/Logger';
import ContactsSelectStore from '../domain/ContactsSelectStore';

const defaultValues = {
  isLoadingGroup: false,
  isLoadingMembers: false,
  isLoadingContactsNotInGroup: false,
  isSaving: false,
  id: undefined,
  name: undefined,
  numberOfContacts: undefined
};

export default class CurrentGroupStore {
  isLoadingGroup = defaultValues.isLoadingGroup;

  isLoadingMembers = defaultValues.isLoadingMembers;

  isLoadingContactsNotInGroup = defaultValues.isLoadingContactsNotInGroup;

  isSaving = defaultValues.isSaving;

  id = defaultValues.id;

  name = defaultValues.name;

  numberOfContacts = defaultValues.numberOfContacts;

  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);
    this.contactsSelectStore = new ContactsSelectStore(api);

    makeAutoObservable(
      this,
      {
        api: false,
        sessionState: false,
        contactsBaseStore: false,
        contactsSelectStore: false
      },
      {
        autoBind: true
      }
    );
  }

  get members() {
    return this.contactsBaseStore.contacts;
  }

  get hasMoreMembers() {
    return this.contactsBaseStore.hasMore;
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

  init = async id => {
    this.id = id;
    this.isLoadingMembers = true;
    await this.loadGroup();
    await this.contactsBaseStore.initContactStore(this.name);
    this.contactsSelectStore.resetSelection();
    this.isLoadingMembers = false;
  };

  *loadGroup() {
    this.isLoadingGroup = true;
    const group = yield this.api.groupsManager.getGroupById(this.id);
    this.name = group.name;
    this.numberOfContacts = group.numberOfContacts;
    this.isLoadingGroup = false;
  }

  fetchMoreGroupMembers = () => {
    this.contactsBaseStore.fetchMoreData();
  };

  reloadMembers = () => this.contactsBaseStore.fetchMoreData({ startFromTheTop: true });

  getContactsNotInGroup = async () => {
    // TODO: we need new API for this
    this.isLoadingContactsNotInGroup = true;
    const allContacts = await this.api.contactsManager.getAllContacts();
    const allMembers = await this.api.contactsManager.getAllContacts({ groupName: this.name });
    const contactIdsInGroup = new Set(allMembers.map(item => item.contactId));
    const contactsNotInGroup = allContacts.filter(item => !contactIdsInGroup.has(item.contactId));
    this.isLoadingContactsNotInGroup = false;
    return contactsNotInGroup;
  };

  *updateGroupName({ newName, onSuccess, onError }) {
    try {
      this.isSaving = true;
      yield this.api.groupsManager.updateGroup(this.id, { newName });
      yield this.loadGroup();
      onSuccess?.();
      this.isSaving = false;
    } catch (error) {
      Logger.error('[CurrentGroupStore.updateGroupName] Error: ', error);
      onError?.();
      this.isSaving = false;
    }
  }

  *updateGroupMembers({ membersUpdate, onSuccess, onError }) {
    try {
      this.isSaving = true;
      yield this.api.groupsManager.updateGroup(this.id, membersUpdate);
      yield this.reloadMembers();
      onSuccess?.();
      this.isSaving = false;
    } catch (error) {
      Logger.error('[CurrentGroupStore.updateGroupMembers] Error: ', error);

      onError?.();
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
    return this.contactsSelectStore.selectAllContacts(ev, { groupName: this.name, searchText });
  }

  resetSelection() {
    return this.contactsSelectStore.resetSelection();
  }

  handleCherryPickSelection(record, selected) {
    return this.contactsSelectStore.handleCherryPickSelection(record, selected);
  }
}
