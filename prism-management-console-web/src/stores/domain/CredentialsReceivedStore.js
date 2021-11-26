import { makeAutoObservable } from 'mobx';

const fallback = {
  credentialsList: []
};
export default class CredentialsReceivedStore {
  isFetching = true;

  credentials = [];

  constructor(api, sessionState) {
    this.api = api;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(
      this,
      {
        api: false,
        transportLayerErrorHandler: false
      },
      { autoBind: true }
    );
  }

  get isLoadingFirstPage() {
    return this.isFetching;
  }

  resetCredentials = () => {
    this.credentials = [];
  };

  /**
   * Note: Credentials received are not paginated.
   * @param {string} contactId - optionally filter credentials by contact
   * @returns {Array} credentials received
   */
  *fetchCredentials(contactId) {
    this.isFetching = true;
    try {
      // second param contactsManager is provided to handle fetching the required contact fields.
      const credentialsReceived = yield this.api.credentialsReceivedManager.getReceivedCredentials(
        contactId,
        this.api.contactsManager
      );

      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.credentials = credentialsReceived;
      this.isFetching = false;
      return credentialsReceived;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCredentials',
        verb: 'getting',
        model: 'Credentials Received'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
      this.isFetching = false;
      return fallback;
    }
  }
}
