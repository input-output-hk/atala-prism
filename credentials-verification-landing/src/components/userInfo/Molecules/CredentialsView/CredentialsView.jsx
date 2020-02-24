import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import CredentialSummary from '../../Atoms/CredentialSummay/CredentialSummary';

const CredentialsView = () => {
  const { t } = useTranslation();
  return (
    <div className="CredentialsView">
      <div className="CredentialsViewIntro">
        <h2>{t('landing.credentialsView.title')}</h2>
        <p>{t('landing.credentialsView.subtitle')}</p>
      </div>
      <CredentialSummary
        credentialText="Government Issued Digital Identity"
        credentialIcon="images/icon-credential-id.svg"
      />
      <CredentialSummary
        credentialText="University Degree"
        credentialIcon="images/icon-credential-university.svg"
      />
      <CredentialSummary
        credentialText="Proof of Employment"
        credentialIcon="images/icon-credential-employment.svg"
      />
      <CredentialSummary
        credentialText="Certificate of Insurance"
        credentialIcon="images/icon-credential-insurance.svg"
      />
    </div>
  );
};

export default CredentialsView;
