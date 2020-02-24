import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';
import CredentialItemLanding from '../../Molecules/CredentialItemLanding/CredentialItemLanding';

const IntroductionInformation = ({ nextStep, buttonDisabled }) => {
  const { t } = useTranslation();

  return (
    <div className="IntroductionInformation">
      <span className="MiniDetailText">
        {t('credential.introductionInformation.miniText')}
        <em>_____</em>
      </span>
      <h1>{t('credential.introductionInformation.title')}</h1>
      <h2>{t('credential.introductionInformation.explanation')}</h2>
      <div className="CredentialItemContainer">
        <CredentialItemLanding
          theme="theme-credential-1"
          credentialImage="images/icon-credential-id.svg"
          credentialName="Goverment Issued Digital Identity"
          credentialIssuer="Department of Interior, Republic of Redland"
        />
      </div>
      <CustomButton
        buttonProps={{
          onClick: nextStep,
          className: 'theme-primary',
          disabled: buttonDisabled
        }}
        buttonText={t('credential.introductionInformation.askForCredential')}
      />
    </div>
  );
};

IntroductionInformation.propTypes = {
  nextStep: PropTypes.func.isRequired,
  buttonDisabled: PropTypes.bool.isRequired
};

export default IntroductionInformation;
