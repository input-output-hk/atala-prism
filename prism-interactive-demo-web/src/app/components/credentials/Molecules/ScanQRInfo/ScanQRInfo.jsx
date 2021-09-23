import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import ScanQRSteps from '../../Atoms/ScanQRSteps/ScanQRSteps';

import './_style.scss';

const ScanQRInfo = ({ currentCredential }) => {
  const { t } = useTranslation();

  return (
    <div className="ScanQRInfo">
      <h1>{t(`credential.scanQRInfo.title.CredentialType${currentCredential}`)} </h1>
      <h3>
        {t('credential.scanQRInfo.explanation')}
        {t(`credential.credentialNames.CredentialType${currentCredential}`)}
        {t('credential.scanQRInfo.explanation2')}
      </h3>
      <div className="StepsContainer">
        <ScanQRSteps />
      </div>
    </div>
  );
};

ScanQRInfo.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default ScanQRInfo;
