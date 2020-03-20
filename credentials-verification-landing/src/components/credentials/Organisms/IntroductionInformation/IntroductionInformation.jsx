import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialItemLanding from '../../../common/Molecules/CredentialItemLanding/CredentialItemLanding';
import './_style.scss';

const IntroductionInformation = ({ nextStep, buttonDisabled, currentCredential }) => {
  const { t } = useTranslation();

  const currentCredentialItem = {
    0: {
      theme: 'theme-credential-1',
      credentialImage: 'images/icon-credential-id.svg',
      credentialName: t('credential.credentialNames.CredentialType0'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType0'),
      credentialDescription: t('credential.credentialDescription.CredentialType0')
    },
    1: {
      theme: 'theme-credential-2',
      credentialImage: 'images/icon-credential-university.svg',
      credentialName: t('credential.credentialNames.CredentialType1'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType1'),
      credentialDescription: t('credential.credentialDescription.CredentialType1')
    },
    2: {
      theme: 'theme-credential-3',
      credentialImage: 'images/icon-credential-employment.svg',
      credentialName: t('credential.credentialNames.CredentialType2'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType2'),
      credentialDescription: t('credential.credentialDescription.CredentialType2')
    },
    3: {
      theme: 'theme-credential-4',
      credentialImage: 'images/icon-credential-insurance.svg',
      credentialName: t('credential.credentialNames.CredentialType3'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType3'),
      credentialDescription: t('credential.credentialDescription.CredentialType3')
    }
  };

  return (
    <div className="IntroductionInformation">
      <span className="MiniDetailText">
        {t('credential.introductionInformation.miniText')}
        <em>_____</em>
      </span>
      <h1>{t('credential.introductionInformation.title')}</h1>
      <h2>{t('credential.introductionInformation.explanation')}</h2>
      <div className="CredentialItemContainer">
        <CredentialItemLanding {...currentCredentialItem[currentCredential]} />
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
  buttonDisabled: PropTypes.bool.isRequired,
  currentCredential: PropTypes.number.isRequired
};

export default IntroductionInformation;
