import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';
import { useTranslation } from 'gatsby-plugin-react-i18next';

const CredentialCard = ({ icon, alt, credential, authority }) => {
  const { t } = useTranslation();
  return (
    <div className="credentialCard">
      <img src={icon} alt={alt} />
      <div className="credentialCardContent">
        <span>{t('credential.CredentialCard.credential')}</span>
        <h3>{credential}</h3>
      </div>
    </div>
  );
};

CredentialCard.propTypes = {
  icon: PropTypes.string.isRequired,
  alt: PropTypes.string.isRequired,
  credential: PropTypes.string.isRequired,
  authority: PropTypes.string.isRequired
};

export default CredentialCard;
