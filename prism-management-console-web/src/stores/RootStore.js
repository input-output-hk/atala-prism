import { message } from 'antd';
import i18n from 'i18next';
import Logger from '../helpers/Logger';
import { PrismStore } from './domain/PrismStore';
import { UiState } from './ui/UiState';

export class RootStore {
  constructor(api) {
    this.prismStore = new PrismStore(api, this);
    this.uiState = new UiState(this);
  }

  handleTransportLayerError = (error, metadata) => {
    // TODO: add account state setters here (CONFIRMED | UNCONFIRMED)
    Logger.error(metadata?.customMessage, error);
    message.error(i18n.t('errors.errorGetting', { model: metadata?.model }));
  };
}
