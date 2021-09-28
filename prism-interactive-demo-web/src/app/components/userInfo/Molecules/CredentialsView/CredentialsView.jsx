import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import './_style.scss';
import CredentialSummary from '../../Atoms/CredentialSummay/CredentialSummary';

const CredentialsView = () => {
  const { t } = useTranslation();
  return (
    <div className="CredentialsView">
      <div className="CredentialsViewIntro">
        <h2>{t('landing.credentialsView.title')}</h2>
      </div>
      <CredentialSummary
        credentialText={t('credential.credentialNames.CredentialType0')}
        credentialIcon="/images/icon-credential-id.svg"
      />
      <CredentialSummary
        credentialText={t('credential.credentialNames.CredentialType1')}
        credentialIcon="/images/icon-credential-university.svg"
      />
      <CredentialSummary
        credentialText={t('credential.credentialNames.CredentialType2')}
        credentialIcon="/images/icon-credential-employment.svg"
      />
      <CredentialSummary
        credentialText={t('credential.credentialNames.CredentialType3')}
        credentialIcon="/images/icon-credential-insurance.svg"
      />
    </div>
  );
};

export default CredentialsView;
