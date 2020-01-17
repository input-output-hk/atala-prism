import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import iconPhone from '../../../../images/icon-phone.svg';

import './_style.scss';

const SendCredentials = ({ nextStep }) => {
  const { t } = useTranslation();

  return (
    <div className="SendCredentials">
      <h1>{t('credential.sendCredentials.title')}</h1>
      <h3>{t('credential.sendCredentials.subtitle')}</h3>
      <div className="SendCredentialContent">
        <img src={iconPhone} alt={t('credential.sendCredential.iconAlt')} />
        <h3 className="TutorialText">
          {t('credential.sendCredentials.howToContinue1')}
          <strong>{t('credential.sendCredentials.receiveCredential')}</strong>
          {t('credential.sendCredentials.howToContinue2')}
        </h3>
        <CustomButton
          buttonProps={{ onClick: nextStep, className: 'theme-primary' }}
          buttonText={t('credential.sendCredentials.receiveCredential')}
        />
      </div>
    </div>
  );
};

SendCredentials.propTypes = {
  nextStep: PropTypes.func.isRequired
};

export default SendCredentials;
