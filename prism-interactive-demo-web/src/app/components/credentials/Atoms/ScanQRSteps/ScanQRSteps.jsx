import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import iconContacts from '../../../../images/touch-connections.svg';

import './_style.scss';

const NUMBERS_OF_STEPS = 3;

const ScanQRSteps = () => {
  const { t } = useTranslation();
  const steps = [];

  const iconedStep = i => (
    <div className="secondStep">
      <div className="LineStep">
        <p>{t(`credential.scanQRInfo.step${i}.part1`)}</p>
        <img src={iconContacts} alt={t(`credential.scanQRInfo.step${i}.alt`)} />
        <p>{t(`credential.scanQRInfo.step${i}.part2`)}</p>
      </div>
      <div className="LineStep">
        <p>{t(`credential.scanQRInfo.step${i}.part3`)}</p>
      </div>
    </div>
  );

  for (let i = 1; i <= NUMBERS_OF_STEPS; i++) {
    const isSecondStep = i === 2;
    const step = isSecondStep ? iconedStep(i) : <p>{t(`credential.scanQRInfo.step${i}`)}</p>;

    steps.push(
      <div className="ScanQRSteps" key={i}>
        <p className="NumberText">{i}.</p>
        {step}
      </div>
    );
  }

  return steps;
};

export default ScanQRSteps;
