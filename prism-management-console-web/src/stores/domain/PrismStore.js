import ContactStore from './ContactStore';
import CredentialIssuedStore from './CredentialIssuedStore';
import CredentialReceivedStore from './CredentialReceivedStore';
import TemplateStore from './TemplateStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.contactStore = new ContactStore(api, rootStore);
    this.credentialIssuedStore = new CredentialIssuedStore(api, rootStore);
    this.credentialReceivedStore = new CredentialReceivedStore(api, rootStore);
    this.templateStore = new TemplateStore(api, rootStore);
  }
}
