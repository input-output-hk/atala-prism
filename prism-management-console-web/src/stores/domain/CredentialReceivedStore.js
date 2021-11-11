import { makeAutoObservable, flow } from 'mobx';
import { credentialReceivedMapper } from '../../APIs/helpers/credentialHelpers';

const defaultValues = {
  isFetching: true,
  credentials: []
};

const fallback = {
  credentialsList: []
};
export default class CredentialReceivedStore {
  isFetching = defaultValues.isFetching;

  credentials = defaultValues.credentials;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      fetchMoreData: flow.bound,
      fetchCredentials: flow.bound,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && !this.credentials.length;
  }

  resetCredentials = () => {
    this.credentials = defaultValues.credentials;
  };

  *fetchMoreData() {
    const response = yield this.fetchCredentials();
    this.credentials = this.credentials.concat(response.credentialsList);
  }

  *fetchCredentials(contactId) {
    this.isFetching = true;
    try {
      const response = yield this.api.credentialsReceivedManager.getReceivedCredentials(contactId);
      const credentialsWithContactsData = response.credentialsList.map(credential =>
        this.api.contactsManager
          .getContact(credential.individualId)
          .then(contactData => ({ contactData, ...credential }))
      );
      const credentialsWithIssuanceProof = yield Promise.all(credentialsWithContactsData);

      const mappedCredentials = credentialsWithIssuanceProof.map(cred =>
        credentialReceivedMapper(cred)
      );

      this.rootStore.handleTransportLayerSuccess();
      this.isFetching = false;
      const mappedResponse = { ...response, credentialsList: mappedCredentials };
      return mappedResponse;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCredentials',
        verb: 'getting',
        model: 'Credentials'
      };
      this.rootStore.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }
}
