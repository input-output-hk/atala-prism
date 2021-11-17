import { message } from 'antd';
import i18n from 'i18next';
import { flow, makeAutoObservable } from 'mobx';

const defaultValues = {
  isLoadingGroup: false,
  isLoadingMembers: false,
  isLoadingContactsNotInGroup: false,
  isSaving: false,
  id: undefined,
  name: undefined,
  members: [],
  numberOfContacts: undefined
};

export default class CurrentGroupState {
  isLoadingGroup = defaultValues.isLoadingGroup;

  isLoadingMembers = defaultValues.isLoadingMembers;

  isLoadingContactsNotInGroup = defaultValues.isLoadingContactsNotInGroup;

  isSaving = defaultValues.isSaving;

  id = defaultValues.id;

  name = defaultValues.name;

  members = defaultValues.members;

  numberOfContacts = defaultValues.numberOfContacts;

  constructor(api, groupStore) {
    this.api = api;
    this.groupStore = groupStore;
    makeAutoObservable(this, {
      loadGroup: flow.bound,
      loadMembers: flow.bound,
      updateGroupName: flow.bound,
      updateGroupMembers: flow.bound,
      groupStore: false
    });
  }

  init = async id => {
    this.id = id;
    this.loadGroup();
    this.loadMembers();
  };

  *loadGroup() {
    this.isLoadingGroup = true;
    const group = yield this.api.groupsManager.getGroupById(this.id);
    this.name = group.name;
    this.numberOfContacts = group.numberOfContacts;
    this.isLoadingGroup = false;
  }

  *loadMembers() {
    this.isLoadingMembers = true;
    this.members = yield this.api.contactsManager.getAllContacts(this.name);
    this.isLoadingMembers = false;
  }

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
    yield this.loadMembers();
    this.isSaving = false;
  }
}
