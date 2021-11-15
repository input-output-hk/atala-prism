import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import CustomButton from '../../../../../components/customButton/CustomButton';
import CredentialItemLanding from '../../../common/Molecules/CredentialItemLanding/CredentialItemLanding';
import './_style.scss';

const IntroductionInformation = ({ nextStep, buttonDisabled, currentCredential }) => {
  const { t } = useTranslation();

  const currentCredentialItem = {
    0: {
      theme: 'theme-credential-1',
      credentialImage: '/images/icon-credential-id.svg',
      credentialName: t('credential.credentialNames.CredentialType0'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType0'),
      credentialDescription: t('credential.credentialDescription.CredentialType0')
    },
    1: {
      theme: 'theme-credential-1',
      credentialImage: '/images/icon-credential-university.svg',
      credentialName: t('credential.credentialNames.CredentialType1'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType1'),
      credentialDescription: t('credential.credentialDescription.CredentialType1')
    },
    2: {
      theme: 'theme-credential-1',
      credentialImage: '/images/icon-credential-employment.svg',
      credentialName: t('credential.credentialNames.CredentialType2'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType2'),
      credentialDescription: t('credential.credentialDescription.CredentialType2')
    },
    3: {
      theme: 'theme-credential-1',
      credentialImage: '/images/icon-credential-insurance.svg',
      credentialName: t('credential.credentialNames.CredentialType3'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType3'),
      credentialDescription: t('credential.credentialDescription.CredentialType3')
    }
  };

  return (
    <div className="IntroductionInformation">
      <h1>{t(`credential.credentialNames.CredentialType${currentCredential}`)}</h1>
      <p>{t(`credential.credentialDescription.CredentialType${currentCredential}`)}</p>
      <div className="CredentialItemContainer">
        <CredentialItemLanding {...currentCredentialItem[currentCredential]} />
      </div>
      <CustomButton
        buttonProps={{
          onClick: nextStep,
          className: 'theme-primary',
          disabled: buttonDisabled
        }}
        buttonText={t(`credential.credentialAsk.CredentialType${currentCredential}`)}
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
