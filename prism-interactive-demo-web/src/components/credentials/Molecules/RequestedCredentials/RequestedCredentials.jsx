import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialCard from '../../Atoms/CredentialCard/CredentialCard';
import {
  UNIVERSITY_DEGREE,
  PROOF_OF_EMPLOYMENT,
  INSURANCE_POLICY
} from '../../../../helpers/constants';
import './_style.scss';

const RequestedCredentials = ({ currentCredential }) => {
  const { t } = useTranslation();

  const idCredential = {
    icon: 'images/icon-credential-id.svg',
    alt: t('credential.credentialNames.CredentialType0'),
    credential: t('credential.credentialNames.CredentialType0'),
    authority: t('credential.credentialIssuers.CredentialType0')
  };
  const universityCredential = {
    icon: 'images/icon-credential-university.svg',
    alt: t('credential.credentialNames.CredentialType1'),
    credential: t('credential.credentialNames.CredentialType1'),
    authority: t('credential.credentialIssuers.CredentialType1')
  };
  const employmentCredential = {
    icon: 'images/icon-credential-employment.svg',
    alt: t('credential.credentialNames.CredentialType2'),
    credential: t('credential.credentialNames.CredentialType2'),
    authority: t('credential.credentialIssuers.CredentialType2')
  };

  const requestedCredentialsByCurrentCredential = {
    [UNIVERSITY_DEGREE]: [idCredential],
    [PROOF_OF_EMPLOYMENT]: [idCredential, universityCredential],
    [INSURANCE_POLICY]: [idCredential, employmentCredential]
  };

  const requestedCredentials = credential =>
    requestedCredentialsByCurrentCredential[credential].map(element => (
      <CredentialCard {...element} />
    ));

  return (
    <div className="RequestedCredentialsContent">
      <div className="RequestedCredentialsTitle">
        <h3>{t('credential.requestedCredentials.title')}</h3>
      </div>
      {requestedCredentials(currentCredential)}
    </div>
  );
};

RequestedCredentials.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default RequestedCredentials;
