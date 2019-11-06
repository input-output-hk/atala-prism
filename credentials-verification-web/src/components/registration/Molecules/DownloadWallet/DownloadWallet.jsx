import React from 'react';
import { Col } from 'antd';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import icon from '../../../../images/registrationMan.svg';

const DownloadWallet = ({ walletError }) => {
  const { t } = useTranslation();

  return (
    <Col>
      <img src={icon} alt={t('registration.alt')} />
      <label>{t('registration.downloadWallet.title')}</label>
      <label>{t('registration.downloadWallet.subtitle')}</label>
      <Link to="Download Wallet" target="_blank" download>
        {t('registration.downloadWallet.action')}
      </Link>
      {walletError && <label>{t('errors.walletNotRunning')}</label>}
    </Col>
  );
};

DownloadWallet.defaultProps = {
  walletError: false
};

DownloadWallet.propTypes = {
  walletError: PropTypes.bool
};

export default DownloadWallet;
