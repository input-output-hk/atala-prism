import React from 'react';
import { Col } from 'antd';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationMan.svg';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const DownloadWallet = ({ walletError }) => {
  const { t } = useTranslation();

  return (
    <div className="DownloadWallet">
      <div className="DownloadContainer">
        <img src={icon} alt={t('registration.alt')} />
        <h2>{t('registration.downloadWallet.title')}</h2>
        <p>{t('registration.downloadWallet.subtitle')}</p>
        <CustomButton
          buttonProps={{
            className: 'theme-secondary'
          }}
          buttonText={t('registration.downloadWallet.action')}
        />
        <Link to="Download Wallet" target="_blank" download>
          {t('registration.downloadWallet.action')}
        </Link>
      </div>
      <div className="ErrorContainer">
        {walletError && <p className="WalletError">{t('errors.walletNotRunning')}</p>}
      </div>
    </div>
  );
};

DownloadWallet.defaultProps = {
  walletError: false
};

DownloadWallet.propTypes = {
  walletError: PropTypes.bool
};

export default DownloadWallet;
