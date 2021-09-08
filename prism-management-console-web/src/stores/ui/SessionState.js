import { message } from 'antd';
import { makeAutoObservable, observable, action } from 'mobx';
import i18n from 'i18next';
import { CONFIRMED, LOADING, LOCKED, SESSION, UNCONFIRMED } from '../../helpers/constants';

export default class SessionState {
  session = { sessionState: LOADING };

  accountStatus = LOADING;

  acceptedModal = false;

  modalIsVisible = false;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      session: observable,
      accountStatus: observable,
      acceptedModal: observable,
      modalIsVisible: observable,
      login: action,
      logout: action,
      showUnconfirmedAccountError: action,
      removeUnconfirmedAccountError: action,
      hideModal: action,
      setSession: false,
      storeSession: false,
      rootStore: false
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

  login = () =>
    this.api.wallet.getSessionFromExtension({ timeout: 5000 }).then(({ sessionData, error }) => {
      if (error) {
        this.setSession({ sessionState: LOCKED });
        message.error(i18n.t(error.message));
      } else this.setSession(sessionData);
    });

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
