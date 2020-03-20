import React from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialItemLanding from '../../../common/Molecules/CredentialItemLanding/CredentialItemLanding';
import { withRedirector } from '../../../providers/withRedirector';
import { RIGHT } from '../../../../helpers/constants';

import './_style.scss';

const FindCredential = ({ redirector: { redirectToUserInfo } }) => {
  const { t } = useTranslation();

  const currentCredentialItem = [
    {
      theme: 'theme-credential-1',
      credentialImage: 'images/icon-credential-id.svg',
      credentialName: t('credential.credentialNames.CredentialType0'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType0'),
      credentialDescription: t('credential.credentialDescription.CredentialType0')
    },
    {
      theme: 'theme-credential-2',
      credentialImage: 'images/icon-credential-university.svg',
      credentialName: t('credential.credentialNames.CredentialType1'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType1'),
      credentialDescription: t('credential.credentialDescription.CredentialType1')
    },
    {
      theme: 'theme-credential-3',
      credentialImage: 'images/icon-credential-employment.svg',
      credentialName: t('credential.credentialNames.CredentialType2'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType2'),
      credentialDescription: t('credential.credentialDescription.CredentialType2')
    },
    {
      theme: 'theme-credential-4',
      credentialImage: 'images/icon-credential-insurance.svg',
      credentialName: t('credential.credentialNames.CredentialType3'),
      credentialIssuer: t('credential.credentialIssuers.CredentialType3'),
      credentialDescription: t('credential.credentialDescription.CredentialType3')
    }
  ];

  const credentialItems = currentCredentialItem.map(credentialItem => (
    <CredentialItemLanding {...credentialItem} />
  ));

  return (
    <div className="FindCredential">
      <div className="TextContainer">
        <span className="MiniDetailText">
          {t('landing.findCredential.detailText')}
          <em>_____</em>
        </span>
        <h1>{t('landing.findCredential.title')}</h1>
        <CustomButton
          buttonProps={{ className: 'theme-primary', onClick: redirectToUserInfo }}
          buttonText={t('landing.findCredential.askForCredential')}
          icon={{ icon: <Icon type="arrow-right" />, side: RIGHT }}
        />
      </div>
      <div className="FindCredentialContainer">{credentialItems}</div>
    </div>
  );
};

FindCredential.propTypes = {
  redirector: PropTypes.shape({ redirectToUserInfo: PropTypes.func }).isRequired
};

export default withRedirector(FindCredential);
