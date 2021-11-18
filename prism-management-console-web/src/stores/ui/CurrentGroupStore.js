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
  // members: [],
  numberOfContacts: undefined
};

export default class CurrentGroupStore {
  isLoadingGroup = defaultValues.isLoadingGroup;

  isLoadingMembers = defaultValues.isLoadingMembers;

  isLoadingContactsNotInGroup = defaultValues.isLoadingContactsNotInGroup;

  isSaving = defaultValues.isSaving;

  id = defaultValues.id;

  name = defaultValues.name;

  // members = defaultValues.members;

  numberOfContacts = defaultValues.numberOfContacts;

  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;

    makeAutoObservable(this, {
      loadGroup: flow.bound,
      loadMembers: flow.bound,
      updateGroupName: flow.bound,
      updateGroupMembers: flow.bound
    });
    // has to be declared after `fetchSearchResults` has been bound.
    // otherwise binding can be forced by passing this.fetchSearchResults.bind(this)
    // this.contactUiState = new ContactUiState({ triggerFetchResults: this.fetchSearchResults });
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);

    // reaction(() => this.contactUiState.textFilter, () => this.contactUiState.triggerSearch());
    // reaction(() => this.contactUiState.statusFilter, () => this.contactUiState.triggerSearch());
    // reaction(() => this.contactUiState.dateFilter, () => this.contactUiState.triggerSearch());
    // reaction(() => this.contactUiState.sortDirection, () => this.contactUiState.triggerSearch());
    // reaction(() => this.contactUiState.sortingBy, () => this.contactUiState.triggerSearch());
  }

  get members() {
    return this.contactsBaseStore.contacts;
  }

  // TODO: should we hide contactsBaseStore or just use it where needed directly!?
  // get memberFilters() {
  //   let {
  //     sortingBy,
  //     sortingKey,
  //     sortDirection,
  //     textFilter,
  //     statusFilter,
  //     dateFilter,
  //     groupNameFilter
  //   } = this.contactsUniversalStore;
  //
  //   return {
  //     sortingBy,
  //     sortingKey,
  //     sortDirection,
  //     textFilter,
  //     statusFilter,
  //     dateFilter,
  //     groupNameFilter
  //   };
  // }
  // get memberFilterActions() {
  //   let {
  //     hasFiltersApplied,
  //     hasMore
  //   } = this.contactsUniversalStore;
  //
  //   return {
  //     sortingBy,
  //     sortingKey,
  //     sortDirection,
  //     textFilter,
  //     statusFilter,
  //     dateFilter,
  //     groupNameFilter
  //   };
  // }

  init = async id => {
    this.id = id;
    this.loadGroup();
    // this.loadMembers();
    this.contactsBaseStore.initContactStore();
  };

  // resetUiState = () => {
  //   this.contactUiState.resetState();
  // };

  *loadGroup() {
    this.isLoadingGroup = true;
    const group = yield this.api.groupsManager.getGroupById(this.id);
    this.name = group.name;
    this.numberOfContacts = group.numberOfContacts;
    this.isLoadingGroup = false;
  }

  // *loadMembers() {
  //   this.isLoadingMembers = true;
  //   this.members = yield this.api.contactsManager.getAllContacts(this.name);
  //   this.isLoadingMembers = false;
  // }

  reloadMembers = () => this.contactsBaseStore.refreshContacts();

  // fetchSearchResults = () => {
  //   FIXME: add fetching group members
  // message.warn('not implemented yet');
  // };

  // FIXME: select only filtered members
  getMembersToSelect = () => this.api.contactsManager.getAllContacts(this.name);

  getContactsNotInGroup = async () => {
    this.isLoadingContactsNotInGroup = true;
    const allContacts = await this.api.contactsManager.getAllContacts();
    const contactIdsInGroup = new Set(this.members.map(item => item.contactId));
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
