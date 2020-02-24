import React from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import CredentialItemLanding from '../../../credentials/Molecules/CredentialItemLanding/CredentialItemLanding';
import { withRedirector } from '../../../providers/withRedirector';
import { RIGHT } from '../../../../helpers/constants';

import './_style.scss';

const FindCredential = ({ redirector: { redirectToUserInfo } }) => {
  const { t } = useTranslation();

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
      <div className="FindCredentialContainer">
        <CredentialItemLanding
          theme="theme-credential-1"
          credentialImage="images/icon-credential-id.svg"
          credentialName="Goverment Issued Digital Identity"
          credentialIssuer="Department of Interior, Republic of Redland"
        />
        <CredentialItemLanding
          theme="theme-credential-2"
          credentialImage="images/icon-credential-university.svg"
          credentialName="University Degree"
          credentialIssuer="Air Side University"
        />
        <CredentialItemLanding
          theme="theme-credential-3"
          credentialImage="images/icon-credential-employment.svg"
          credentialName="Proof of  Employment"
          credentialIssuer="Atala Inc."
        />
        <CredentialItemLanding
          theme="theme-credential-4"
          credentialImage="images/icon-credential-insurance.svg"
          credentialName="Certificate of Insurance"
          credentialIssuer="Atala Insurance Ltd."
        />
      </div>
    </div>
  );
};

FindCredential.propTypes = {
  redirector: PropTypes.shape({ redirectToUserInfo: PropTypes.func }).isRequired
};

export default withRedirector(FindCredential);
