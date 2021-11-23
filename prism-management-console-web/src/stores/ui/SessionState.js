import { message } from 'antd';
import { makeAutoObservable, flow } from 'mobx';
import i18n from 'i18next';
import { CONFIRMED, LOADING, LOCKED, SESSION, UNCONFIRMED } from '../../helpers/constants';
import TransportLayerErrorHandler from '../TransportLayerErrorHandler';

export default class SessionState {
  session = { sessionState: LOADING };

  accountStatus = LOADING;

  acceptedModal = false;

  modalIsVisible = false;

  constructor(api) {
    this.api = api;
    this.transportLayerErrorHandler = new TransportLayerErrorHandler(this);

    makeAutoObservable(this, {
      login: flow.bound,
      setSession: false,
      storeSession: false
    });
    this.api.wallet.setSessionErrorHandler(this.handleSessionError);
    this.login();
  }

  setSession = newSession => {
    this.session = newSession;
    this.storeSession();
  };

  storeSession = () => {
    localStorage.setItem(SESSION, JSON.stringify(this.session));
    this.api.wallet.setSessionState(this.session);
  };

  *login() {
    const { sessionData, error } = yield this.api.wallet.getSessionFromExtension({ timeout: 5000 });
    if (error) {
      this.setSession({ sessionState: LOCKED });
      message.error(i18n.t(error.message));
    } else this.setSession(sessionData);
  }

  logout = () => {
    this.setSession({ sessionState: LOCKED });
  };

  showUnconfirmedAccountError = () => {
    this.accountStatus = UNCONFIRMED;
    if (!this.acceptedModal) {
      this.modalIsVisible = true;
    }
  };

  removeUnconfirmedAccountError = () => {
    this.accountStatus = CONFIRMED;
  };

  hideModal = () => {
    this.acceptedModal = true;
    this.modalIsVisible = false;
  };

  handleSessionError = () => {
    message.error(i18n.t('errors.walletLockedOrRemoved'));
    this.logout();
  };
}
