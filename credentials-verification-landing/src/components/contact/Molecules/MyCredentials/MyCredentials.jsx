import React from 'react';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import CredentialSummary from '../../Atoms/CredentialSummay/CredentialSummary';

const MyCredentials = () => {
  const { t } = useTranslation();
  const credentialsList = [
    {
      key: t('credential.credentialNames.CredentialType0'),
      credentialText: t('credential.credentialNames.CredentialType0'),
      credentialIcon: 'images/icon-credential-id.svg'
    },
    {
      key: t('credential.credentialNames.CredentialType1'),
      credentialText: t('credential.credentialNames.CredentialType1'),
      credentialIcon: 'images/icon-credential-university.svg'
    },
    {
      key: t('credential.credentialNames.CredentialType2'),
      credentialText: t('credential.credentialNames.CredentialType2'),
      credentialIcon: 'images/icon-credential-employment.svg'
    },
    {
      key: t('credential.credentialNames.CredentialType3'),
      credentialText: t('credential.credentialNames.CredentialType3'),
      credentialIcon: 'images/icon-credential-insurance.svg'
    }
  ];
  const credentialsSummary = credentialsList.map(credential => (
    <CredentialSummary {...credential} />
  ));

  return (
    <div className="CredentialsView">
      <div className="CredentialsViewIntro">
        <h2>{t('landing.MyCredentials.title')}</h2>
        <h2>{t('landing.MyCredentials.title2')}</h2>
      </div>
      {credentialsSummary}
    </div>
  );
};

export default MyCredentials;
