import React from 'react';
import { useTranslation } from 'react-i18next';
import ScanQRSteps from '../../Atoms/ScanQRSteps/ScanQRSteps';

import './_style.scss';

const ScanQRInfo = () => {
  const { t } = useTranslation();

  return (
    <div className="ScanQRInfo">
      <h1>{t('credential.scanQRInfo.title')} </h1>
      <h3>{t('credential.scanQRInfo.explanation')}</h3>
      <div className="StepsContainer">
        <ScanQRSteps />
      </div>
    </div>
  );
};

export default ScanQRInfo;
