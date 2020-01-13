import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Row } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';
import ScanQRSteps from '../../Atoms/ScanQRSteps/ScanQRSteps';

const ScanQRInfo = ({ nextStep }) => {
  const { t } = useTranslation();

  return (
    <div className="ScanQRInfo">
      <h1>
        <strong>{t('credential.scanQRInfo.title')}</strong>
      </h1>
      <Row>
        <label>{t('credential.scanQRInfo.explanation')}</label>
      </Row>
      <ScanQRSteps />
    </div>
  );
};

ScanQRInfo.propTypes = {
  nextStep: PropTypes.func.isRequired
};

export default ScanQRInfo;
