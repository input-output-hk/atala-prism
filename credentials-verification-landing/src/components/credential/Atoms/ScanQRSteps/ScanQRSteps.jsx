import React from 'react';
import { useTranslation } from 'react-i18next';
import { Row } from 'antd';

const ScanQRSteps = () => {
  const { t } = useTranslation();
  const steps = [];

  for (let i = 0; i < 3; i++) {
    steps.push(
      <Row>
        <p>{i + 1}.</p>
        <strong>{t(`credential.scanQRInfo.step${i + 1}`)}</strong>
      </Row>
    );
  }

  return steps;
};

export default ScanQRSteps;
