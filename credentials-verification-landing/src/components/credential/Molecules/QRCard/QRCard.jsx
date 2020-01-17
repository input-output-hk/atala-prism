import React from 'react';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { useTranslation } from 'react-i18next';
import DownloadButtons from '../../../landing/Molecules/DownloadButtons/DownloadButtons';

import './_style.scss';

const QRCard = ({ qrValue }) => {
  const { t } = useTranslation();

  return (
    <div className="QRCard">
      <QRCode value={qrValue} />
      <div className="QRCardText">
        <h3>
          <strong>{t('credential.QRCard.noAppYet')}</strong>
        </h3>
        <h3>{t('credential.QRCard.downloadIt')}</h3>
      </div>
      <DownloadButtons />
    </div>
  );
};

QRCard.propTypes = {
  qrValue: PropTypes.string.isRequired
};

export default QRCard;
