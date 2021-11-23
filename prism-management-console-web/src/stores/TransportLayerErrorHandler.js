import { message } from 'antd';
import i18n from 'i18next';
import { UNKNOWN_DID_SUFFIX_ERROR_CODE } from '../helpers/constants';
import Logger from '../helpers/Logger';

export default class TransportLayerErrorHandler {
  constructor(sessionState) {
    this.sessionState = sessionState;
  }

  handleTransportLayerSuccess = logMessage => {
    const { removeUnconfirmedAccountError } = this.sessionState;
    removeUnconfirmedAccountError();
    if (logMessage) Logger.info(logMessage);
  };

  handleTransportLayerError = (error, metadata = {}) => {
    const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = this.sessionState;

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
