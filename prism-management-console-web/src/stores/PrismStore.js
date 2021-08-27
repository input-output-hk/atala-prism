import { createContext } from 'react';
import { makeAutoObservable } from 'mobx';
import { TemplateStore } from './TempalteStore';

export class PrismStore {
  constructor(api, rootStore) {
    this.tempalteStore = new TemplateStore(api, rootStore);
  }
}

export const PrismStoreContext = createContext({});
