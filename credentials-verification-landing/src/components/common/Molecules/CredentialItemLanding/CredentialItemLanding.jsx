import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import './_style.scss';

const CredentialItemLanding = ({
  theme,
  credentialImage,
  credentialName,
  credentialIssuer,
  credentialDescription
}) => {
  const { t } = useTranslation();
  const className = `CredentialItemLandingDetail ${theme}`;
  return (
    <div className="CredentialItemLanding">
      <div className={className}>
        <img src={credentialImage} alt="CredentialIcon" />
        <div className="CredentialDescription">
          <span>{t('landing.credential.credential')}</span>
          <h3>{credentialName}</h3>
          <p>{credentialDescription}</p>
          <span>{t('landing.credential.IssuingAuthority')}</span>
          <h3>{credentialIssuer}</h3>
        </div>
      </div>
    </div>
  );
};

CredentialItemLanding.propTypes = {
  theme: PropTypes.string.isRequired,
  credentialImage: PropTypes.string.isRequired,
  credentialName: PropTypes.string.isRequired,
  credentialIssuer: PropTypes.string.isRequired,
  credentialDescription: PropTypes.string.isRequired
};

export default CredentialItemLanding;
