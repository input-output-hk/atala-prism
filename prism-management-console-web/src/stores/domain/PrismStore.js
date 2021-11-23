import CredentialReceivedStore from './CredentialReceivedStore';
import TemplateStore from './TemplateStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.credentialReceivedStore = new CredentialReceivedStore(api, rootStore);
    this.templateStore = new TemplateStore(api, rootStore);
  }
}
