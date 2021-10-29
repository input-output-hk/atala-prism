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

  handleTransportLayerSuccess = logMessage => {
    const { removeUnconfirmedAccountError } = this.uiState.sessionState;
    removeUnconfirmedAccountError();
    if (logMessage) Logger.info(logMessage);
  };

  handleTransportLayerError = (error, metadata = {}) => {
    const {
      showUnconfirmedAccountError,
      removeUnconfirmedAccountError
    } = this.uiState.sessionState;

    if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
      showUnconfirmedAccountError();
    } else {
      removeUnconfirmedAccountError();
      const customMessage = `[${metadata.store}.${metadata.method}] 
        Error while ${metadata.verb} ${metadata.model}`;

      Logger.error(customMessage, error);
      message.error(i18n.t(`errors.${metadata.verb}`, { model: metadata.model }));
    }
  };
}
