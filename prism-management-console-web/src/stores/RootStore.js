import { message } from 'antd';
import i18n from 'i18next';
import { UNKNOWN_DID_SUFFIX_ERROR_CODE } from '../helpers/constants';
import Logger from '../helpers/Logger';
import { PrismStore } from './domain/PrismStore';
import { UiState } from './ui/UiState';

export class RootStore {
  constructor(api) {
    this.prismStore = new PrismStore(api, this);
    this.uiState = new UiState(api, this);
  }

  handleTransportLayerError = (error, metadata) => {
    const {
      showUnconfirmedAccountError,
      removeUnconfirmedAccountError
    } = this.uiState.sessionState;

    if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
      showUnconfirmedAccountError();
    } else {
      removeUnconfirmedAccountError();
      Logger.error(metadata?.customMessage, error);
      message.error(i18n.t('errors.errorGetting', { model: metadata?.model }));
    }
  };
}
