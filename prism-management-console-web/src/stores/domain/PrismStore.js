import { createContext } from 'react';
import TemplateStore from './TemplateStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.templateStore = new TemplateStore(api, rootStore);
  }
}

export const PrismStoreContext = createContext({});
