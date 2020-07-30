import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationMan.svg';

import './_style.scss';

const DownloadWallet = ({ walletError }) => {
  const { t } = useTranslation();

  return (
    <div className="DownloadWallet">
      <div className="DownloadContainer">
        <img src={icon} alt={t('registration.alt')} />
        <h2>{t('registration.downloadWallet.title')}</h2>
        <p>{t('registration.downloadWallet.subtitle')}</p>
        <Link className="theme-secondary" to="Download Wallet" target="_blank" download>
          {t('registration.downloadWallet.action')}
        </Link>
      </div>
      <div className="ErrorContainer">
        {walletError && <p className="WalletError">{t(walletError.message)}</p>}
      </div>
    </div>
  );
};

DownloadWallet.defaultProps = {
  walletError: null
};

DownloadWallet.propTypes = {
  walletError: PropTypes.shape({
    message: PropTypes.string
  })
};

export default DownloadWallet;
