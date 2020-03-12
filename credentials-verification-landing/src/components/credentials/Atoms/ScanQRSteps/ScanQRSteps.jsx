import React from 'react';
import { useTranslation } from 'react-i18next';
import iconConnection from '../../../../images/touch-connections.svg';

import './_style.scss';

const ScanQRSteps = () => {
  const { t } = useTranslation();
  const steps = [];

  const secondStep = (
    <div className="secondStep">
      <p>{t('credential.scanQRInfo.step2.part1')}</p>
      <img src={iconConnection} alt={t('credential.scanQRInfo.step2.alt')} />
      <p>{t('credential.scanQRInfo.step2.part2')}</p>
    </div>
  );

  for (let i = 0; i < 3; i++) {
    const step = i === 1 ? secondStep : <p>{t(`credential.scanQRInfo.step${i + 1}`)}</p>;

    steps.push(
      <div className="ScanQRSteps" key={i}>
        <p className="NumberText">{i + 1}.</p>
        {step}
      </div>
    );
  }

  return steps;
};

export default ScanQRSteps;
