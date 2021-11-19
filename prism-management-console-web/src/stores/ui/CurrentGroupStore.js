import { message } from 'antd';
import i18n from 'i18next';
import { flow, makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../ContactsBaseStore';

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

    makeAutoObservable(this, {
      loadGroup: flow.bound,
      updateGroupName: flow.bound,
      updateGroupMembers: flow.bound
    });
  }

  get members() {
    return this.contactsBaseStore.contacts;
  }

  get hasFiltersApplied() {
    return this.contactsBaseStore.hasFiltersApplied;
  }

  get hasMoreMembers() {
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

  init = async id => {
    this.id = id;
    this.isLoadingMembers = true;
    await this.loadGroup();
    await this.contactsBaseStore.initContactStore(this.name);
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

  reloadMembers = () => this.contactsBaseStore.refreshContacts();

  // FIXME: select only filtered members
  getMembersToSelect = () => this.api.contactsManager.getAllContacts(this.name);

  getContactsNotInGroup = async () => {
    this.isLoadingContactsNotInGroup = true;
    const allContacts = await this.api.contactsManager.getAllContacts();
    const allMembers = await this.api.contactsManager.getAllContacts(this.name);
    const contactIdsInGroup = new Set(allMembers.map(item => item.contactId));
    const contactsNotInGroup = allContacts.filter(item => !contactIdsInGroup.has(item.contactId));
    this.isLoadingContactsNotInGroup = false;
    return contactsNotInGroup;
  };

  *updateGroupName(newName) {
    this.isSaving = true;
    yield this.api.groupsManager.updateGroup(this.id, { newName });
    message.success(i18n.t('groupEditing.success'));
    yield this.loadGroup();
    this.isSaving = false;
  }

  *updateGroupMembers(membersUpdate) {
    this.isSaving = true;
    yield this.api.groupsManager.updateGroup(this.id, membersUpdate);
    message.success(i18n.t('groupEditing.success'));
    yield this.reloadMembers();
    this.isSaving = false;
  }
}
