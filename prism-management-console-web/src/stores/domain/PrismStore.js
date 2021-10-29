import { createContext } from 'react';
import GroupStore from './GroupStore';
import TemplateStore from './TemplateStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.groupStore = new GroupStore(api, rootStore);
    this.templateStore = new TemplateStore(api, rootStore);
  }
}

export const PrismStoreContext = createContext({});
