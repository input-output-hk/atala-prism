import TemplateStore from './TemplateStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.templateStore = new TemplateStore(api, rootStore);
  }
}
