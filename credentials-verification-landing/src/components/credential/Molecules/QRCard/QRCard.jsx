import React from 'react';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { useTranslation } from 'react-i18next';

import './_style.scss';
import DownloadButtons from '../../../landing/Molecules/DownloadButtons/DownloadButtons';

const QRCard = ({ qrValue }) => {
  const { t } = useTranslation();

  return (
    <div className="QRCard">
      <QRCode value={qrValue} />
      <strong>{t('credential.QRCard.noAppYet')}</strong>
      <label>{t('credential.QRCard.downloadIt')}</label>
      <DownloadButtons />
    </div>
  );
};

QRCard.propTypes = {
  qrValue: PropTypes.string.isRequired
};

export default QRCard;
