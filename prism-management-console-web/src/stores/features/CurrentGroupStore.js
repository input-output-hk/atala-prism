import { message } from 'antd';
import i18n from 'i18next';
import { flow, makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';

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

  reloadMembers = () => this.contactsBaseStore.fetchMoreData({ startFromTheTop: true });

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
    try {
      this.isSaving = true;
      yield this.api.groupsManager.updateGroup(this.id, { newName });
      // TODO: this logic belongs to views, here we should just set a flag.
      //  This way, the store is coupled with antd lib.
      message.success(i18n.t('groupEditing.success'));
      yield this.loadGroup();
      this.isSaving = false;
    } catch {
      message.error(i18n.t('groupEditing.errors.grpc'));
      this.isSaving = false;
    }
  }

  *updateGroupMembers(membersUpdate) {
    try {
      this.isSaving = true;
      yield this.api.groupsManager.updateGroup(this.id, membersUpdate);
      // TODO: same thing as for *updateGroupName
      message.success(i18n.t('groupEditing.success'));
      yield this.reloadMembers();
      this.isSaving = false;
    } catch {
      message.error(i18n.t('groupEditing.errors.grpc'));
      this.isSaving = false;
    }
  }
}
