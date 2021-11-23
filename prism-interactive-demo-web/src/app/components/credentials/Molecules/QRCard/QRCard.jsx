import React from 'react';
import PropTypes from 'prop-types';
import QRCode from 'qrcode.react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import DownloadButtons from '../../../../../components/landing/Molecules/DownloadButtons/DownloadButtons';

import './_style.scss';

const QRCard = ({ qrValue, showDownloadHelp }) => {
  const { t } = useTranslation();

  return (
    <div className="QRCard">
      <QRCode value={qrValue} />
      {showDownloadHelp && (
        <div className="QRCardContent">
          <div className="QRCardText">
            <h3>{t('credential.QRCard.noAppYet')}</h3>
            <p>{t('credential.QRCard.downloadIt')}</p>
          </div>
          <DownloadButtons />
        </div>
      )}
    </div>
  );
};

QRCard.propTypes = {
  qrValue: PropTypes.string.isRequired,
  showDownloadHelp: PropTypes.bool
};

QRCard.defaultProps = {
  showDownloadHelp: false
};

export default QRCard;
