import { makeAutoObservable } from 'mobx';

const defaultValues = {
  isLoadingContact: false,
  isLoadingGroups: false,
  isLoadingCredentialsIssued: false,
  isLoadingCredentialsReceived: false,
  isSaving: false,
  contactId: undefined,
  externalId: undefined,
  contactName: undefined,
  groups: [],
  credentialsIssued: [],
  credentialsReceived: []
};

export default class CurrentContactState {
  isLoadingContact = defaultValues.isLoadingContact;

  isLoadingContact = defaultValues.isLoadingContact;

  isLoadingGroups = defaultValues.isLoadingGroups;

  isLoadingCredentialsIssued = defaultValues.isLoadingCredentialsIssued;

  isLoadingCredentialsReceived = defaultValues.isLoadingCredentialsReceived;

  isSaving = defaultValues.isSaving;

  contactId = defaultValues.contactId;

  externalId = defaultValues.externalId;

  contactName = defaultValues.contactName;

  groups = defaultValues.groups;

  credentialsIssued = defaultValues.credentialsIssued;

  credentialsReceived = defaultValues.credentialsReceived;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      rootStore: false
    });
  }

  // initialization

  init = contactId => {
    this.contactId = contactId;
    this.loadContact();
    this.loadGroups();
    this.loadCredentialsIssued();
    this.loadCredentialsReceived();
  };

  loadContact = async () => {
    this.isLoadingContact = true;
    const { fetchContactById } = this.rootStore.prismStore.contactStore;

    const contact = await fetchContactById(this.contactId);

    this.contactName = contact.contactName;
    this.externalId = contact.externalId;
    this.isLoadingContact = false;
  };

  loadGroups = async () => {
    this.isLoadingGroups = true;
    const { getContactGroups } = this.rootStore.prismStore.groupStore;
    const groups = await getContactGroups(this.contactId);

    this.groups = groups;
    this.isLoadingGroups = false;
  };

  loadCredentialsIssued = async () => {
    this.isLoadingCredentialsIssued = true;
    const { getContactCredentials } = this.rootStore.prismStore.credentialIssuedStore;

    const credentials = await getContactCredentials(this.contactId);

    this.credentialsIssued = credentials;
    this.isLoadingCredentialsIssued = false;
  };

  loadCredentialsReceived = async () => {
    this.isLoadingCredentialsReceived = true;
    const { fetchCredentials } = this.rootStore.prismStore.credentialReceivedStore;

    const { credentialsList } = await fetchCredentials(this.contactId);

    this.credentialsReceived = credentialsList;
    this.isLoadingCredentialsReceived = false;
  };
}
