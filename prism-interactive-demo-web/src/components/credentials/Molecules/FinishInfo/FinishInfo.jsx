import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import homeAppIcon from '../../../../images/touch-connections.svg';
import './_style.scss';

const FinishInfo = ({ currentCredential }) => {
  const { t } = useTranslation();

  return (
    <div className="FinishInfo">
      <h1>
        <strong>{t('credential.finishInfo.congrats')}</strong>
      </h1>
      <h3>{t('credential.finishInfo.explanation')}{t(`credential.credentialNames.CredentialType${currentCredential}`)}.</h3>
      <div className="FinishContent">
        <div className="StepTextItem">
          <p className="NumberText">1.</p>
          <p>{t('credential.finishInfo.openOnPhone')}</p>
          <img src={homeAppIcon} alt="Home Icon" />
          <p>{t('credential.finishInfo.checkHome2')}</p>
        </div>
        <div className="StepTextItem">
          <p className="NumberText">2.</p>
          <p>{t('credential.finishInfo.checkHome')}</p>
        </div>
      </div>
    </div>
  );
};

FinishInfo.propTypes = {
  confirmSuccessCredential: PropTypes.func.isRequired,
  currentCredential: PropTypes.number.isRequired
};

export default FinishInfo;
