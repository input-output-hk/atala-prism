import { makeAutoObservable, observable, action } from 'mobx';

const defaultValues = {
  isLoadingGroup: false,
  isLoadingMembers: false,
  isSaving: false,
  id: undefined,
  name: undefined,
  members: [],
  numberOfContacts: undefined
};

export default class CurrentGroupState {
  isLoadingGroup = defaultValues.isLoadingGroup;

  isLoadingMembers = defaultValues.isLoadingMembers;

  isSaving = defaultValues.isSaving;

  id = defaultValues.id;

  name = defaultValues.name;

  members = defaultValues.members;

  numberOfContacts = defaultValues.numberOfContacts;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      isLoadingGroup: observable,
      isLoadingMembers: observable,
      isSaving: observable,
      id: observable,
      name: observable,
      members: observable,
      numberOfContacts: observable,
      init: action,
      refreshGroupMembers: action,
      updateGroupName: action,
      updateGroupMembers: action,
      rootStore: false
    });
  }

  init = async id => {
    this.isLoadingGroup = true;
    this.id = id;
    const { getGroupById } = this.rootStore.prismStore.groupStore;

    const group = await getGroupById(id);
    this.name = group.name;
    this.numberOfContacts = group.numberOfContacts;
    this.refreshGroupMembers();
    this.isLoadingGroup = false;
  };

  refreshGroupMembers = async () => {
    this.isLoadingMembers = true;
    const { fetchAllContacts } = this.rootStore.prismStore.contactStore;
    const members = await fetchAllContacts(this.name);
    this.members = members;
    this.isLoadingMembers = false;
  };

  updateGroupName = async newName => {
    this.isSaving = true;
    const { updateGroup } = this.rootStore.prismStore.groupStore;
    await updateGroup(this.id, { newName });
    this.refreshGroupMembers();
    this.isSaving = false;
  };

  updateGroupMembers = async membersUpdate => {
    this.isSaving = true;
    const { updateGroup } = this.rootStore.prismStore.groupStore;
    await updateGroup(this.id, membersUpdate);
    this.refreshGroupMembers();
    this.isSaving = false;
  };

  getContactsNotInGroup = async () => {
    const { fetchAllContacts } = this.rootStore.prismStore.contactStore;
    const allContacts = await fetchAllContacts();
    const contactIdsInGroup = new Set(this.members.map(item => item.contactId));
    const contactsNotInGroup = allContacts.filter(item => !contactIdsInGroup.has(item.contactId));
    return contactsNotInGroup;
  };
}
